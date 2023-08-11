package com.tianji.learning.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static com.tianji.learning.contants.RedisContants.SIGN_RECORD_KEY_PREFIX;


@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;

    /**
     * 签到
     */
    @Override
    public SignResultVO addSignRecords() {
        // 1. 获取用户id
        Long userId = UserContext.getUser();

        // 2. 保存签到数据到redis中
        DateTime date = DateUtil.date();
        int year = DateUtil.year(date);
        int month = DateUtil.month(date);
        int day = DateUtil.dayOfMonth(date);
        String time = String.valueOf(year) + String.valueOf(month);
        String key = SIGN_RECORD_KEY_PREFIX + userId.toString() + time;

        Boolean bit = redisTemplate.opsForValue().setBit(key, day - 1, true);
        if (bit) {
            throw new BadRequestException("不能重复签到");
        }

        // 3. 从redis中获取该用户的签到数据,计算连续签到的天数
        int signDays = countSignDays(key, day);

        // 3.计算签到得分
        int rewardPoints = 0;
        switch (signDays) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }
        // 4.保存积分明细记录
        mqHelper.send(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints + 1));// 签到积分是基本得分+奖励积分

        // 5.封装返回
        SignResultVO vo = new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;

    }

    @Override
    public Byte[] querySignRecords() {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.获取日期
        LocalDate now = LocalDate.now();
        int dayOfMonth = now.getDayOfMonth();
        // 3.拼接key
        String key = SIGN_RECORD_KEY_PREFIX
                + userId
                + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        // 4.读取
        List<Long> result = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return new Byte[0];
        }
        int num = result.get(0).intValue();

        Byte[] arr = new Byte[dayOfMonth];
        int pos = dayOfMonth - 1;
        while (pos >= 0){
            arr[pos--] = (byte)(num & 1);
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return arr;
    }

    private int countSignDays(String key, int day) {

        List<Long> result = redisTemplate
                .opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return 0;
        }
        int num = result.get(0).intValue(); // 十进制数
        // 2.定义一个计数器
        int count = 0;
        // 3.循环，与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while ((num & 1) == 1) {
            // 4.计数器+1
            count++;
            // 5.把数字右移一位，最后一位被舍弃，倒数第二位成了最后一位
            num >>>= 1;
        }
        return num;
    }

}
