package com.tianji.promotion.service;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author zcm
 * @since 2023-08-17
 */
public interface IUserCouponService extends IService<UserCoupon> {


    /**
     * 用户领取优惠券
     * */
    void receiveCoupon(Long couponId);


    /**
     * 兑换码兑换优惠券
     * */
    void exchangeCoupon(String code);



    /**
     * 校验并生成用户券，更新兑换码状态
     * */
    void checkAndCreateUserCoupon(Coupon coupon, Long userId, Integer serialNum);
}
