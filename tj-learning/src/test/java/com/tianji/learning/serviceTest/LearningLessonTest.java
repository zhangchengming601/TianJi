package com.tianji.learning.serviceTest;


import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

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


    @Test
    public void test3(){
        List<Person> personList = List.of(
                new Person("Alice", 25),
                new Person("Bob", 30),
                new Person("Charlie", 22)
        );

        List<Person> updatedPersonList = personList.stream()
                .peek(person -> person.setName(person.getName().toUpperCase()))
                .collect(Collectors.toList());


        System.out.println("----------begin-----------");
        updatedPersonList.forEach(person -> System.out.println(person.getName() + ", " + person.getAge()));
        System.out.println("personList="+personList.get(0).name);
    }


    static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }
    }
}
