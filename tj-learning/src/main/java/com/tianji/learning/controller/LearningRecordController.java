package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author zcm
 * @since 2023-07-29
 */
@RestController
@RequestMapping("/learning-records")
public class LearningRecordController {

    @Autowired
    ILearningRecordService learningRecordService;

    @GetMapping("/course/{courseId}")
    LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId){
        return learningRecordService.queryLearningRecordByCourse(courseId);
    }

    @ApiOperation("提交学习记录")
    @PostMapping
    public void submitLearnRecord(@RequestBody @Validated LearningRecordFormDTO learningRecordFormDTO){
        learningRecordService.submitLearnRecord(learningRecordFormDTO);
    }

}
