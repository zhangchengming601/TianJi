package com.tianji.promotion.service.impl;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import static com.tianji.promotion.constants.PromotionConstants.COUPON_CODE_SERIAL_KEY;
import static com.tianji.promotion.constants.PromotionConstants.COUPON_RANGE_KEY;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-08-15
 */
@Service
@RequiredArgsConstructor
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 异步生成兑换码
     * */
    @Async("asyncTaskExecutor")
    @Override
    public void asyncGenerateExchangeCode(Coupon coupon) {
        // 获得该优惠券需要发放的数量
        Integer totalNum = coupon.getTotalNum();

        // 1. 生成自增id 借助redis的INCRBY命令
        Long endId = redisTemplate.opsForValue().increment(COUPON_CODE_SERIAL_KEY, totalNum);
        Long startId = endId - totalNum + 1;
        // 2. 调用工具类，生成兑换码
        ArrayList<ExchangeCode> list = new ArrayList<>();
        for (Long i=startId;i<=endId;i++){
            // 这里虽然第二个参数用的是coupon.getId() ；  Long类型；   在CodeUtil的generateCode会对该Long类型进行处理
            String code = CodeUtil.generateCode(i, coupon.getId());

            ExchangeCode exchangeCode = new ExchangeCode();
            exchangeCode.setCode(code);
            exchangeCode.setExchangeTargetId(coupon.getId());
            exchangeCode.setId(i.intValue());
            exchangeCode.setExpiredTime(coupon.getIssueEndTime());
            list.add(exchangeCode);
        }

        // 3. 保存兑换码到DB （exchangeCode表）；批量保存
        saveBatch(list);

        // 4.写入Redis缓存，member：couponId，score：兑换码的最大序列号
        redisTemplate.opsForZSet().add(COUPON_RANGE_KEY, coupon.getId().toString(), endId);
    }


    /**
     * 根据兑换码id，校验该兑换码是否已经被兑换过了
     * */
    @Override
    public boolean updateExchangeMark(long serialNum, boolean mark) {
        Boolean boo = redisTemplate.opsForValue().setBit(COUPON_RANGE_KEY, serialNum, mark);
        return boo != null && boo;
    }


}

