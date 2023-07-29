package com.tianji.learning.serviceTest;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.service.ILearningRecordService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class LearningRecordTest {

    @Autowired
    ILearningRecordService learningRecordService;

    @Test
    public void test1(){
        UserContext.setUser(2l);

        LearningLessonDTO learningLessonDTO = learningRecordService.queryLearningRecordByCourse(2l);

        log.info("learningLessonDTO={}",learningLessonDTO.toString());

    }
}
