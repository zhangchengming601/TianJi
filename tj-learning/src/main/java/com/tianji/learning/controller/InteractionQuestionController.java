package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author zcm
 * @since 2023-08-07
 */

@Api(tags = "互动问题相关接口")
@RestController
@RequestMapping("/questions")
@AllArgsConstructor
public class InteractionQuestionController {

    private final IInteractionQuestionService questionService;

    @ApiOperation("新增互动问题")
    @PostMapping
    public void saveQuestion(@Validated @RequestBody QuestionFormDTO questionFormDTO){
        questionService.saveQuestion(questionFormDTO);
    }


    @ApiOperation("修改互动问题")
    @PutMapping("{id}")
    public void updateQuestion(
            @PathVariable("id") Long id,
            @RequestBody QuestionFormDTO questionFormDTO){
        questionService.updateQuesion(id,questionFormDTO);
    }


    @ApiOperation("用户端分页查询问题")
    @GetMapping("page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query){
        return questionService.queryQuestionPage(query);
    }

    @ApiOperation("根据id查询问题详情")
    @GetMapping("/{id}")
    public QuestionVO queryQuestionById(@ApiParam(value = "问题id", example = "1") @PathVariable("id") Long id){
        return questionService.queryQuestionById(id);
    }

}
