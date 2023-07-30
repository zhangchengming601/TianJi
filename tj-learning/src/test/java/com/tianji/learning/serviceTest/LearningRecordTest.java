package com.tianji.learning.serviceTest;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningRecordService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@SpringBootTest
public class LearningRecordTest {

    @Autowired
    ILearningRecordService learningRecordService;

    @Autowired
    LearningRecordMapper learningRecordMapper;

    @Test
    public void test1(){
        UserContext.setUser(2l);

        LearningLessonDTO learningLessonDTO = learningRecordService.queryLearningRecordByCourse(2l);

        log.info("learningLessonDTO={}",learningLessonDTO.toString());

    }


    @Test
    public void test2(){
        DateTime date = DateUtil.date();
        DateTime begin = DateUtil.beginOfWeek(date);
        DateTime end = DateUtil.endOfWeek(date);
        List<IdAndNumDTO> idAndNumDTOS = learningRecordMapper.countLearnedSections(2l, begin.toLocalDateTime(), end.toLocalDateTime());
        log.info("idAndNumDTOS={}",idAndNumDTOS);

        List<IdAndNumDTO> idAndNumDTOS1 = learningRecordMapper.countAllLearnedSections(2l);

        log.info("idAndNumDTOS1={}",idAndNumDTOS1);
    }
}
