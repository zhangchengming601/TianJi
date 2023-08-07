package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.AssertUtils;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.entity.LearningLesson;
import com.tianji.learning.entity.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-07-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {


    final CourseClient courseClient;
    final CatalogueClient catalogueClient;

    @Autowired LearningLessonMapper learningLessonMapper;
    @Autowired
    LearningRecordMapper learningRecordMapper;


    /**添加课程信息到用户课程表*/
    @Override
    public void addUserLessons(Long userId, List<Long> courseIds) {

        //使用courseIds通过fegin远程调用tj-course服务，查出该课程的有效期
        List<CourseSimpleInfoDTO> courseSimpleInfoDTOList = courseClient.getSimpleInfoList(courseIds);
        if(courseSimpleInfoDTOList.isEmpty()){
            log.error("课程信息不存在，无法添加到课表");
            return;
        }


        List<LearningLesson> lessons = new ArrayList<>();
        // 封装实体类为PO
        for(CourseSimpleInfoDTO csid : courseSimpleInfoDTOList){
            LearningLesson learningLesson = new LearningLesson();
            learningLesson.setUserId(userId);
            learningLesson.setCourseId(csid.getId());

            Integer validDuration = csid.getValidDuration();
            if(validDuration != null && validDuration > 0){
                LocalDateTime now = LocalDateTime.now();
                learningLesson.setCreateTime(now);
                // 计算课程的过期时间
                learningLesson.setExpireTime(now.plusMonths(validDuration));
            }
            lessons.add(learningLesson);
        }

        // 保存课程信息到用户的课程表中
        saveBatch(lessons);
    }


    /**
     *
     * 分页查询我的课程表
     *
     * */
    @Override
    public PageDTO<LearningLessonVO> queryMyLessonPage(PageQuery pageQuery) {
        // 拿到当前登录的用户id
        Long userId = UserContext.getUser();

        // 在learning_lesson表中根据用户id，分页查询数据
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)  // where user_id = #{userId}
                .page(pageQuery.toMpPage("latest_learn_time", false));


        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        Map<Long, CourseSimpleInfoDTO> courseSimpleInfoDTOMap = queryCourseInfoList(records);

        //封装数据，返回给前端
        List<LearningLessonVO> list = new ArrayList<>();
        for (LearningLesson lesson : records){
            LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
            CourseSimpleInfoDTO csid = courseSimpleInfoDTOMap.get(lesson.getCourseId());
            vo.setCourseName(csid.getName());
            vo.setCourseCoverUrl(csid.getCoverUrl());
            vo.setSections(csid.getSectionNum());
            list.add(vo);
        }

        return PageDTO.of(page,list);
    }



    /**
     * 查询正在(最近)学习的课程信息
     * */
    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        Long userId = UserContext.getUser();

        // 查询正在学习的课程 select * from xx where user_id = #{userId} AND status = 1 order by latest_learn_time limit 1
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if(lesson == null){
            return null;
        }
        // 拷贝LearningLesson到LearningLessonVO
        LearningLessonVO learningLessonVO = BeanUtil.copyProperties(lesson, LearningLessonVO.class);

        CourseFullInfoDTO course = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (course ==null){
            throw new BadRequestException("课程不存在");
        }
        learningLessonVO.setCourseName(course.getName());
        learningLessonVO.setCourseCoverUrl(course.getCoverUrl());
        learningLessonVO.setSections(course.getSectionNum());

        // 5.统计课表中的课程数量 select count(1) from xxx where user_id = #{userId}
        Integer courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        learningLessonVO.setCourseAmount(courseAmount);

        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getCourseId()));
        if (!cataSimpleInfoDTOS.isEmpty()){
            CataSimpleInfoDTO csid = cataSimpleInfoDTOS.get(0);
            learningLessonVO.setLatestSectionName(csid.getName());
            learningLessonVO.setLatestSectionIndex(csid.getCIndex());
        }
        return learningLessonVO;
    }


    /**
     * 查询当前用户对于该课程是否有权观看
     * */
    @Override
    public Long queryLessonVaild(Long courseId) {
        // 1. 获得当前用户的id
        Long userId = UserContext.getUser();

        // 2. 根据用户id和courseId查询数据库中是否有这条记录 select * from table where userId = xxx and courseId  = xxx
        LambdaQueryWrapper<LearningLesson> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LearningLesson::getUserId,userId);
        queryWrapper.eq(LearningLesson::getCourseId,courseId);
        LearningLesson lesson = learningLessonMapper.selectOne(queryWrapper);
        if(lesson == null){
            return null;
        }

        // 3. 判断该课程是否在有效期内
        if(lesson.getExpireTime().isBefore(LocalDateTime.now())){
            return null;
        }
        return courseId;
    }


    /**
     * 根据课程id，查询当前用户的课表中是否有该课程，如果有该课程则需要返回课程的学习进度、课程有效期等信息。
     * */
    @Override
    public LearningLessonVO queryHasLessonById(Long courseId) {
        Long userId = UserContext.getUser();
        LambdaQueryWrapper<LearningLesson> queryWrapper = new LambdaQueryWrapper<LearningLesson>()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId);
        LearningLesson learningLesson = learningLessonMapper.selectOne(queryWrapper);
        if(learningLesson != null){
            return BeanUtils.copyBean(learningLesson, LearningLessonVO.class);
        }
        return null;
    }

    @Override
    public void createLearningPlan(Long courseId, Integer freq) {
        // 1.获取当前登录的用户
        Long userId = UserContext.getUser();
        // 2.查询课表中的指定课程有关的数据
        LearningLesson lesson = queryByUserAndCourseId(userId, courseId);
        AssertUtils.isNotNull(lesson, "课程信息不存在！");
        // 3.修改数据
        LearningLesson l = new LearningLesson();
        l.setId(lesson.getId());
        l.setWeekFreq(freq);
        if(lesson.getPlanStatus() == PlanStatus.NO_PLAN) {
            l.setPlanStatus(PlanStatus.PLAN_RUNNING);
        }
        updateById(l);
    }


    /**
     * 查询学习计划进度
     * */
    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        Long userId = UserContext.getUser();

        // 1. 计算本周的开始时间和结束时间
        DateTime now = DateUtil.date();
        DateTime beginOfWeek = DateUtil.beginOfWeek(now);
        DateTime endOfWeek = DateUtil.endOfWeek(now);

        // 2. 在learning_lesson表中查询当前用户的本周的计划学习小节数量--条件是本周当前用户正在学习和未开始的课程
        Page<LearningLesson> page = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus,PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.LEARNING, LessonStatus.NOT_BEGIN)
                .page(query.toMpPage());
        List<LearningLesson> records = page.getRecords();
        if (records.isEmpty()){
            return null;
        }
        List<LearningPlanVO> vos = BeanUtil.copyToList(records, LearningPlanVO.class);
        // 本周计划学习的所有章节数
        Integer allPlanNum = vos.stream()
                .filter(learningPlanVO -> learningPlanVO.getWeekFreq() != null)
                .map(learningPlanVO -> learningPlanVO.getWeekFreq())
                .reduce(0, (result, element) -> result + element);

        // feign; 远程查询课程信息，返回值是List集合
        List<Long> courseIds = vos.stream().map(LearningPlanVO::getCourseId).collect(Collectors.toList());
        if (!CollUtil.isEmpty(courseIds)){
            List<CourseSimpleInfoDTO> courseList = courseClient.getSimpleInfoList(courseIds);
            Map<Long, CourseSimpleInfoDTO> coursesMap = courseList.stream()
                    .collect(Collectors.toMap(courseSimpleInfoDTO -> courseSimpleInfoDTO.getId(), courseSimpleInfoDTO -> courseSimpleInfoDTO));
            if (!CollUtil.isEmpty(coursesMap)){
                // 通过查询到的数据对VO进行赋值
                List<LearningPlanVO> list = vos.stream()
                        .peek(learningPlanVO -> learningPlanVO.setCourseName(coursesMap.get(learningPlanVO.getCourseId()).getName()))
                        .peek(learningPlanVO -> learningPlanVO.setSections(coursesMap.get(learningPlanVO.getCourseId()).getSectionNum()))
                        .collect(Collectors.toList());
            }
        }


        // 3. 在learning_record表中查询单个lesson本周已经完成的小节数量和总完成的数量

        // 本周的每个lesson_id的已完成的小节数量
        List<IdAndNumDTO> weekNum = learningRecordMapper.countLearnedSections(userId,
                beginOfWeek.toLocalDateTime(),
                endOfWeek.toLocalDateTime());
        Map<Long, Integer> weekNumMap = IdAndNumDTO.toMap(weekNum);
        // 本周完成的小节总数
        Integer weekFinNum = weekNum.stream().
                map(idAndNumDTO -> idAndNumDTO.getNum())
                .reduce(0, (result, element) -> result + element);

        // 每个lesson_id已完成小节数量 (不是本周)
        List<IdAndNumDTO> allNum = learningRecordMapper.countAllLearnedSections(userId);
        Map<Long, Integer> allNumMap = IdAndNumDTO.toMap(allNum);
        // 总共完成的小节数量(不是本周)
        Integer AllFinNum = allNum.stream().
                map(idAndNumDTO -> idAndNumDTO.getNum())
                .reduce(0, (result, element) -> result + element);

        if(CollUtil.isNotEmpty(weekNumMap)){
            vos.stream()
                    .peek(learningPlanVO ->
                            learningPlanVO.setWeekLearnedSections(weekNumMap.get(learningPlanVO.getId())!=null?weekNumMap.get(learningPlanVO.getId()):0))
                    .collect(Collectors.toList());
        }
        if(CollUtil.isNotEmpty(allNumMap)){
            vos.stream()
                    .peek(learningPlanVO ->
                            learningPlanVO.setLearnedSections(allNumMap.get(learningPlanVO.getId())!=null?allNumMap.get(learningPlanVO.getId()):0))
                    .collect(Collectors.toList());
        }

//        Integer weekFinNum = 0;
//        for(LearningPlanVO learningPlanVO : vos){
//            // 对每一个lessonId，去learning_record中查找所有已经完成的小节集合
//            Long lessonId = learningPlanVO.getId();
//            LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<LearningRecord>()
//                    .eq(LearningRecord::getLessonId, lessonId)
//                    .eq(LearningRecord::getFinished,true);
//            List<LearningRecord> learningRecords = learningRecordMapper.selectList(wrapper);
//
//            // 将查询结果分为本周已完成的小节数量和总共已完成的小节数量
//            int allFinishedSize = learningRecords.size();
//            long thisWeekFinishedSize = learningRecords.stream()
//                    .filter(learningRecord -> learningRecord.getFinishTime().isAfter(beginOfWeek.toLocalDateTime()) && learningRecord.getFinishTime().isBefore(endOfWeek.toLocalDateTime()))
//                    .count();
//            weekFinNum+=(int)thisWeekFinishedSize;
//
//            learningPlanVO.setWeekLearnedSections((int) thisWeekFinishedSize);
//            learningPlanVO.setLearnedSections(allFinishedSize);
//        }

        // TODO: 积分查询

        LearningPlanPageVO learningPlanPageVO = new LearningPlanPageVO();
        learningPlanPageVO.setWeekTotalPlan(allPlanNum);
        learningPlanPageVO.setWeekFinished(weekFinNum);

        return learningPlanPageVO.pageInfo(page.getTotal(),page.getPages(),vos);
    }

    public LearningLesson queryByUserAndCourseId(Long userId, Long coueseId){
        return this.lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getCourseId,coueseId)
                .one();
    }


    /**
     * 根据List<LearningLesson> 查询cources信息
     * 返回值是一个Map，其中key是course的id ； value是Course本身的信息
     * */
    private Map<Long, CourseSimpleInfoDTO> queryCourseInfoList(List<LearningLesson> lessons){

        // lessons的id集合
        Set<Long> cIds = lessons.stream() // 创建流
                .map(LearningLesson::getCourseId)  //中间操作 ： 这里获得每个LearningLesson对象courseId
                .collect(Collectors.toSet());  // 终结操作 ， 把当前流转换成一个集合

        // 使用fegin远程获取course集合
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(cInfoList)) {
            // 课程不存在，无法添加
            throw new BadRequestException("课程信息不存在！");
        }

        // 对List进行转换为Map
        Map<Long, CourseSimpleInfoDTO> result = cInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, courseSimpleInfoDTO -> courseSimpleInfoDTO));

        return result;
    }
}
