package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.entity.InteractionReply;

/**
 * <p>
 * 互动问题的回答或评论 服务类
 * </p>
 *
 * @author zcm
 * @since 2023-08-07
 */
public interface IInteractionReplyService extends IService<InteractionReply> {

    /**
     * 新增回答或者评论
     * */
    void saveReply(ReplyDTO replyDTO);


    /**
     * 分页查询回答或者评论列表
     * */
    PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query,boolean forAdmin);
}
