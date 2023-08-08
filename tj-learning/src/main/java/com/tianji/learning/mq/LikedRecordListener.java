package com.tianji.learning.mq;

import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.learning.entity.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.QA_LIKED_TIMES_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikedRecordListener {

    private final IInteractionReplyService replyService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.liked.times.queue",durable = "true"),
            exchange = @Exchange(name = LIKE_RECORD_EXCHANGE,type = ExchangeTypes.TOPIC),
            key =QA_LIKED_TIMES_KEY ))
    public void listenReplyLikedTimesChange(LikedTimesDTO dto){
        log.debug("监听到回答或评论{}的点赞数变更:{}", dto.getBizId(), dto.getLikedTimes());
        // 1. 判断dto非空
        if (dto ==null){
            throw new BadRequestException("监听到空的消息");
        }
        // 2. 更新数据库
        InteractionReply reply = replyService.getById(dto.getBizId());
        if (reply==null){
            return;
        }
        reply.setLikedTimes(dto.getLikedTimes());
        replyService.updateById(reply);
    }
}
