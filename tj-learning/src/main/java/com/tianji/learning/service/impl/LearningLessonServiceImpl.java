package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.entity.LearningLesson;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
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
