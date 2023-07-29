package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.entity.LearningLesson;
import com.tianji.learning.entity.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
