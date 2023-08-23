package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import com.tianji.promotion.utils.MyLock;
import com.tianji.promotion.utils.MyLockStrategy;
import com.tianji.promotion.utils.MyLockType;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static com.tianji.common.constants.MqConstants.Exchange.PROMOTION_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.COUPON_RECEIVE;
import static com.tianji.promotion.constants.PromotionConstants.COUPON_CACHE_KEY_PREFIX;
import static com.tianji.promotion.constants.PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX;

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
    private final IExchangeCodeService codeService;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;


    /**
     * 用户领取优惠券
     * */
    @Override
    @Transactional
    @MyLock(name = "lock:coupon:uid:#{id}")
    public void receiveCoupon(Long couponId) {
        // 1.查询优惠券
        // Coupon coupon = couponMapper.selectById(couponId);
        Coupon coupon = queryCouponByRedisCache(couponId);

        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        // 2.校验发放时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
            throw new BadRequestException("优惠券发放已经结束或尚未开始");
        }
        // 3.校验库存
        if (coupon.getTotalNum()<=0) {
            throw new BadRequestException("优惠券库存不足");
        }


        Long userId = UserContext.getUser();
        // 通过redisson来实现分布式锁
        String key = "lock:coupon:uid:" + userId;
//        RLock lock = redissonClient.getLock(key);
//
//        try {
//            boolean isLock = lock.tryLock();
//            if (!isLock) {
//                throw  new BizIllegalException("操作太频繁了");
//            }
//            // 获得IUserCouponService的代理对象
//            IUserCouponService userCouponServiceProxy = (IUserCouponService) AopContext.currentProxy();
//            userCouponServiceProxy.checkAndCreateUserCoupon(coupon, userId, null);
//        } finally {
//            lock.unlock();
//        }


//        // 4.校验并生成用户券
//        synchronized (userId.toString().intern()) {
//            // 获得IUserCouponService的代理对象
//            IUserCouponService userCouponServiceProxy = (IUserCouponService) AopContext.currentProxy();
//            userCouponServiceProxy.checkAndCreateUserCoupon(coupon, userId, null);
//        }


/*        // 获得IUserCouponService的代理对象
        IUserCouponService userCouponServiceProxy = (IUserCouponService) AopContext.currentProxy();
        userCouponServiceProxy.checkAndCreateUserCoupon(coupon, userId, null);*/

        // 统计已领数量
        String userCouponKey = USER_COUPON_CACHE_KEY_PREFIX + couponId;
        Long increment = redisTemplate.opsForHash().increment(userCouponKey, userId, 1);
        if (increment>coupon.getUserLimit()) {
            throw new BizIllegalException("当前用户超出限领数量");
        }

        // 在redis中修改优惠券库存  -1
        redisTemplate.opsForHash().increment(COUPON_CACHE_KEY_PREFIX+couponId,"totalNum",-1);

        // 发送消息到mq，消息内容是 userId,couponId
        UserCouponDTO msg = new UserCouponDTO();
        msg.setCouponId(couponId);
        msg.setUserId(userId);
        mqHelper.send(PROMOTION_EXCHANGE,COUPON_RECEIVE,msg);
    }

    /**
     * 从redis中获取优惠券信息
     * */
    private Coupon queryCouponByRedisCache(Long couponId){
        // 1. key
        String key = COUPON_CACHE_KEY_PREFIX + couponId;
        // 2. 从redis中查询数据
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        // 3. 将map类型转为Bean类型
        Coupon coupon = BeanUtil.mapToBean(entries, Coupon.class, false, CopyOptions.create());
        return coupon;
    }


/*
    *//**
     * 校验并生成用户券，更新兑换码状态
     * *//*
    @Transactional
    @Override
    @MyLock(name = "lock:coupon:uid:#{userId}",
            lockType = MyLockType.RE_ENTRANT_LOCK,
            lockStrategy = MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT
    )
    public void checkAndCreateUserCoupon(Coupon coupon, Long userId, Integer serialNum){
        // 1.校验每人限领数量
        // 1.1.统计当前用户对当前优惠券的已经领取的数量
        Integer count = lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, coupon.getId())
                .count();
        // 1.2.校验限领数量
        if(count != null && count >= coupon.getUserLimit()){
            throw new BadRequestException("超出领取数量");
        }
        // 2.更新优惠券的已经发放的数量 + 1
        int num = couponMapper.incrIssueNum(coupon.getId());
        if (num==0) {
            throw new BizIllegalException("优惠券库存不足");
        }
        // 3.新增一个用户券
        saveUserCoupon(coupon, userId);
        // 4.更新兑换码状态
        if (serialNum != null) {
            codeService.lambdaUpdate()
                    .set(ExchangeCode::getUserId, userId)
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .eq(ExchangeCode::getId, serialNum)
                    .update();
        }
    }*/

    @Transactional
    @MyLock(name = "lock:coupon:uid:#{userId}",
            lockType = MyLockType.RE_ENTRANT_LOCK,
            lockStrategy = MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT
    )
    public void checkAndCreateUserCoupon(Coupon coupon, Long userId, Integer serialNum){
        // 1.校验每人限领数量
        // 1.1.统计当前用户对当前优惠券的已经领取的数量
        Integer count = lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, coupon.getId())
                .count();
        // 1.2.校验限领数量
        if(count != null && count >= coupon.getUserLimit()){
            throw new BadRequestException("超出领取数量");
        }
        // 2.更新优惠券的已经发放的数量 + 1
        int num = couponMapper.incrIssueNum(coupon.getId());
        if (num==0) {
            throw new BizIllegalException("优惠券库存不足");
        }
        // 3.新增一个用户券
        saveUserCoupon(coupon, userId);
        // 4.更新兑换码状态
        if (serialNum != null) {
            codeService.lambdaUpdate()
                    .set(ExchangeCode::getUserId, userId)
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .eq(ExchangeCode::getId, serialNum)
                    .update();
        }
    }


    /**
     * 校验并生成用户券，更新兑换码状态
     * */
    @Transactional
    @Override
    public void checkAndCreateUserCoupon(UserCouponDTO uc) {
        // 1.查询优惠券
        Coupon coupon = couponMapper.selectById(uc.getCouponId());
        if (coupon == null) {
            return;
        }
        // 2.更新优惠券的已经发放的数量 + 1
        int r = couponMapper.incrIssueNum(coupon.getId());
        if (r == 0) {
            return;
        }
        // 3.新增一个用户券
        saveUserCoupon(coupon, uc.getUserId());
       /* // 4.更新兑换码状态
        if (serialNum != null) {
            codeService.lambdaUpdate()
                    .set(ExchangeCode::getUserId, uc.getUserId())
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .eq(ExchangeCode::getId, uc.getSerialNum())
                    .update();
        }*/
    }


    /**
     * 兑换码兑换优惠券
     * */
    @Override
    @Transactional
    public void exchangeCoupon(String code) {
        // 1. 根据兑换码，通过CodeUtil解析出该用户的目标优惠券信息
        long exchangeCodeId = CodeUtil.parseCode(code);

        // 2. 校验该兑换码是否已经兑换过（redis的bitmap）
        boolean isExchanged = codeService.updateExchangeMark(exchangeCodeId, true);
        if (isExchanged) {
            throw new BizIllegalException("兑换码已经被兑换过了");
        }

        // 3. 校验该优惠券是否可用（是否过期，是否还有剩余，是否超出用户的限领数量）
        try {
            // 3.查询兑换码对应的优惠券id
            ExchangeCode exchangeCode = codeService.getById(exchangeCodeId);
            if (exchangeCode == null) {
                throw new BizIllegalException("兑换码不存在！");
            }
            // 4.是否过期
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(exchangeCode.getExpiredTime())) {
                throw new BizIllegalException("兑换码已经过期");
            }
            // 5.校验并生成用户券
            // 5.1.查询优惠券
            Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
            // 5.2.查询用户
            Long userId = UserContext.getUser();
            // 5.3.校验并生成用户券，更新兑换码状态
            checkAndCreateUserCoupon(coupon, userId, (int)exchangeCodeId);
        } catch (Exception e) {
            // 重置兑换的标记 0
            codeService.updateExchangeMark(exchangeCodeId, false);
            throw e;
        }


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
