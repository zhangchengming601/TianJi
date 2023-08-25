package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 Mapper 接口
 * </p>
 *
 * @author zcm
 * @since 2023-08-17
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    // 我们用Coupon中的creater字段（Long）来存储UserCoupon的id字段; UserCoupon的id字段是为了在使用了该优惠券之后，在UserCoupon表中将该条数据改为已使用
    List<Coupon> queryMyCoupons(@Param("userId") Long userId);

}
