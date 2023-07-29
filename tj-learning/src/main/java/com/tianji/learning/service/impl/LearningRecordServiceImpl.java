package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.entity.LearningLesson;
import com.tianji.learning.entity.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-07-29
 */
@Service
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    @Autowired
    ILearningLessonService learningLessonService;

    @Autowired
    CourseClient courseClient;



    /**
     * 查询该课程的学习记录
     * */
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        Long userId = UserContext.getUser();

        LambdaQueryWrapper<LearningLesson> queryWrapper = new LambdaQueryWrapper<LearningLesson>()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId);

        LearningLesson learningLesson = learningLessonService.getOne(queryWrapper);
        if(learningLesson==null){
            throw new BizIllegalException("该课程未加入课表");
        }

        LearningLessonDTO learningLessonDTO = new LearningLessonDTO();
        learningLessonDTO.setId(learningLesson.getId());
        learningLessonDTO.setLatestSectionId(learningLesson.getLatestSectionId());
        learningLessonDTO.setRecords(new ArrayList<>());

        LambdaQueryWrapper<LearningRecord> learningRecordLambdaQueryWrapper = new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getLessonId, learningLesson.getId());
        List<LearningRecord> learningRecordList = this.list(learningRecordLambdaQueryWrapper);
        learningRecordList.stream()
                .forEach(learningRecord -> learningLessonDTO.getRecords().add(BeanUtils.copyBean(learningRecord, LearningRecordDTO.class)));

//        for(LearningRecord lr: learningRecordList){
//            LearningRecordDTO learningRecordDTO = BeanUtils.copyBean(lr, LearningRecordDTO.class);
//            learningLessonDTO.getRecords().add(learningRecordDTO);
//        }

        return learningLessonDTO;
    }

    /**
     * 提交学习记录
     * */
    @Transactional
    @Override
    public void submitLearnRecord(LearningRecordFormDTO dto) {
        Long userId = UserContext.getUser();

        // 1. 首先检查数据库中是否已经存在这条learningRecord的记录
        LearningRecord learningRecord = this.lambdaQuery()
                .eq(LearningRecord::getLessonId, dto.getLessonId())
                .eq(LearningRecord::getSectionId, dto.getSectionId())
                .one();

        boolean isFinished = lessonIsFinished(dto);
        boolean isFinishedInDb = false;

        // 2. 更新或者插入该learningRecord
        if (learningRecord == null){
            // 2.1 如果数据库中没有记录，则插入
            insertRecordToDb(dto, userId, learningRecord, isFinished);
        }else{
            isFinishedInDb = learningRecord.getFinished();
            // 2.2 如果数据库中有这条记录，则更新
            updataRecordToDb(dto, learningRecord, isFinished);
        }

        // 3. 更新learning_lesson表中的字段
        updataLessonToDb(dto, learningRecord, isFinished,isFinishedInDb);
    }


    private void updataLessonToDb(LearningRecordFormDTO learningRecordFormDTO,
                                  LearningRecord learningRecord,
                                  boolean isFinished,
                                  boolean isFinishedInDb) {
        LearningLesson learningLesson = learningLessonService.lambdaQuery()
                .eq(LearningLesson::getId, learningRecordFormDTO.getLessonId())
                .one();
        if(learningLesson == null){
            throw new BizIllegalException("没有该课程信息");
        }
        learningLesson.setLatestSectionId(learningRecordFormDTO.getSectionId());
        learningLesson.setLatestLearnTime(LocalDateTime.now());
        LessonStatus status = learningLesson.getStatus();
        if(!isFinishedInDb
                &&isFinished
                && (status == LessonStatus.NOT_BEGIN || status == LessonStatus.LEARNING)){
            CourseFullInfoDTO courseInfoById = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
            if(courseInfoById == null){
                throw new BizIllegalException("没有该课程信息");
            }
            if(courseInfoById.getSectionNum()>learningLesson.getLearnedSections()+1){
                if (status == LessonStatus.NOT_BEGIN){
                    learningLesson.setStatus(LessonStatus.LEARNING);
                }
                learningLesson.setLearnedSections(learningLesson.getLearnedSections()+1);

            }else{
                learningLesson.setLearnedSections(learningLesson.getLearnedSections()+1);
                learningLesson.setStatus(LessonStatus.FINISHED);
            }
        }
        learningLessonService.updateById(learningLesson);
    }


    private void updataRecordToDb(LearningRecordFormDTO learningRecordFormDTO, LearningRecord learningRecord, boolean isFinished) {
        learningRecord.setMoment(learningRecordFormDTO.getMoment());
        if(!learningRecord.getFinished()){
            learningRecord.setFinished(isFinished);
            if(isFinished){
                learningRecord.setFinishTime(learningRecordFormDTO.getCommitTime());
            }
        }
        this.updateById(learningRecord);
    }

    private void insertRecordToDb(LearningRecordFormDTO learningRecordFormDTO,
                                  Long userId,
                                  LearningRecord learningRecord,
                                  boolean isFinished) {
        LearningRecord record = BeanUtil.copyProperties(learningRecordFormDTO, LearningRecord.class);
        record.setUserId(userId);
        record.setFinished(isFinished);
        record.setUpdateTime(learningRecordFormDTO.getCommitTime());
        this.save(record);
    }


    /**
     * 判断该课程是否已经完成
     * */
    public boolean lessonIsFinished(LearningRecordFormDTO learningRecordFormDTO){
        if(learningRecordFormDTO.getSectionType()==SectionType.VIDEO){
            Integer duration = learningRecordFormDTO.getDuration();
            Integer moment = learningRecordFormDTO.getMoment();
            return moment*2>=duration;
        }
        return true;
    }
}
