package com.tianji.promotion.service;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author zcm
 * @since 2023-08-15
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {


    /**
     * 异步生成兑换码
     * */
    void asyncGenerateExchangeCode(Coupon coupon);


    /**
     * 根据兑换码id，校验该兑换码是否已经被兑换过了
     * */
    boolean updateExchangeMark(long serialNum, boolean mark);

}
