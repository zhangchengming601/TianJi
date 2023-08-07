package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.entity.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author zcm
 * @since 2023-08-07
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {


    /**
     * 新增互动问题
     * */
    void saveQuestion(QuestionFormDTO questionDTO);


    /**
     * 修改互动问题
     * */
    void updateQuesion(Long id,QuestionFormDTO questionFormDTO);



    /**
     * 用户端分页查询问题
     * */
    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);


    /**
     * 根据问题id查询问题详情
     * */
    QuestionVO queryQuestionById(Long id);


    /**
     * 管理端分页查询问题
     * */
    PageDTO<QuestionAdminVO> queryQuestionAdminVOPage(QuestionAdminPageQuery query);
}
