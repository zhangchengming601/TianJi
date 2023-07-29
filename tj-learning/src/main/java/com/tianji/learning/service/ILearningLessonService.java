package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.entity.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author zcm
 * @since 2023-07-22
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    /**添加课程信息到用户课程表*/
    void addUserLessons(Long userId , List<Long> courseIds);

    /**分页查询我的课程表*/
    PageDTO<LearningLessonVO> queryMyLessonPage(PageQuery pageQuery);


    /**
     * 查询正在(最近)学习的课程信息
     * */
    LearningLessonVO queryMyCurrentLesson();


    /**
     * 查询当前用户对于该课程是否有权观看
     * */
    Long queryLessonVaild(Long courseId);


    /**
     * 根据课程id，查询当前用户的课表中是否有该课程，如果有该课程则需要返回课程的学习进度、课程有效期等信息。
     * */
    LearningLessonVO queryHasLessonById(Long courseId);

}
