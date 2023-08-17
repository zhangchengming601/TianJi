package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-08-17
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;

    /**
     * 用户领取优惠券
     * */
    @Override
    public void receiveCoupon(Long couponId) {
        Long userId = UserContext.getUser();

        Coupon coupon = couponMapper.selectById(couponId);
        Integer count = this.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId)
                .count();
        // 判断当前优惠券是否已经发放完了 该优惠券（已发行数量 < 总数量）
        // 判断是否超过该优惠券的发放时间
        // 判断该优惠券是否正在发放
        // 判断是否超过每人限领数
        if (!(coupon.getIssueNum()<coupon.getTotalNum())
                || LocalDateTime.now().isAfter(coupon.getIssueEndTime())
                || coupon.getStatus()!= CouponStatus.ISSUING
                || coupon.getUserLimit()<=count) {
            throw new BadRequestException("领取该优惠券失败");
        }

        // 5.更新优惠券的已经发放的数量 + 1
        couponMapper.incrIssueNum(coupon.getId());
        // 6.新增一个用户券
        saveUserCoupon(coupon, userId);
    }

    private void saveUserCoupon(Coupon coupon, Long userId) {
        // 1.基本信息
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(coupon.getId());
        // 2.有效期信息
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if (termBeginTime == null) {
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
        }
        uc.setTermBeginTime(termBeginTime);
        uc.setTermEndTime(termEndTime);
        // 3.保存
        save(uc);
    }
}
