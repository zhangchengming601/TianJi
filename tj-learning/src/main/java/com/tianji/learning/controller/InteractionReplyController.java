package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author zcm
 * @since 2023-08-07
 */
@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
public class InteractionReplyController {

    private final IInteractionReplyService replyService;

    @ApiOperation("新增回答或者评论")
    @PostMapping
    public void saveReply(@RequestBody ReplyDTO replyDTO){
        replyService.saveReply(replyDTO);
    }


    @ApiOperation("分页查询回答或者评论列表")
    @GetMapping("/page")
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query){
        return replyService.queryReplyPage(query,false);
    }


}
