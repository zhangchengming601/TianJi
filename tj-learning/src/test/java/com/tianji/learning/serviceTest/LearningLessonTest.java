package com.tianji.learning.serviceTest;


import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class LearningLessonTest {

    @Autowired
    ILearningLessonService learningLessonService;

    @Test
    public void test1(){
        UserContext.setUser(2l);
        LearningLessonVO learningLessonVO = learningLessonService.queryMyCurrentLesson();
        System.out.println(learningLessonVO.toString());
    }


    @Test
    public void test2(){
        UserContext.setUser(2l);
        Long vaild = learningLessonService.queryLessonVaild(5l);
        System.out.println(vaild);
    }
}
