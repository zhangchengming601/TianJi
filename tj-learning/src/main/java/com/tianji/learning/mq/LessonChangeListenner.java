package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;

import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.Valid;

import static com.tianji.common.constants.MqConstants.Exchange.ORDER_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.ORDER_PAY_KEY;

@Component
@Slf4j
@RequiredArgsConstructor
public class LessonChangeListenner {

    private final ILearningLessonService lessonService;

    /**
     * 监听订单支付或课程报名的消息
     * @param order 订单信息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue",durable = "true"),
            exchange = @Exchange(name = ORDER_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = {ORDER_PAY_KEY}
    ))
    public void listenLessonPay(OrderBasicDTO order){

        // 检查数据是否缺失
        if(order == null || order.getOrderId() == null || order.getCourseIds().isEmpty()){
            // 数据不全，不能处理
            log.error("接收到MQ消息有误，订单数据为空");
            return;
        }

        log.debug("监听到用户{}的订单{}，需要添加课程{}到课表中", order.getUserId(), order.getOrderId(), order.getCourseIds());
        lessonService.addUserLessons(order.getUserId(), order.getCourseIds());
    }

}
