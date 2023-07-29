package com.tianji.learning.service;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.entity.LearningRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author zcm
 * @since 2023-07-29
 */
public interface ILearningRecordService extends IService<LearningRecord> {

    /**
     * 查询该课程的学习记录
     * */
    LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId);

}
