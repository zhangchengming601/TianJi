package com.tianji.promotion.constants;

public interface PromotionConstants {

    /**
     * 兑换券 自增长id ，对应的键
     * */
    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";

    /**
     * 校验兑换码是否兑换，  借助redis的bitmap数据结构
     * */
    String COUPON_CODE_MAP_KEY = "coupon:code:map";

    /**
     * 优惠券缓存前缀  "prs:coupon: + 优惠券id"
     * hash结构
     * */
    String COUPON_CACHE_KEY_PREFIX = "prs:coupon:";

    /**
     * 用户优惠券 缓存前缀 prs:user:coupon:+优惠券id
     * hash结构
     * */
    String USER_COUPON_CACHE_KEY_PREFIX = "prs:user:coupon:";
    String COUPON_RANGE_KEY = "coupon:code:range";

    String[] RECEIVE_COUPON_ERROR_MSG = {
            "活动未开始",
            "库存不足",
            "活动已经结束",
            "领取次数过多",
    };
    String[] EXCHANGE_COUPON_ERROR_MSG = {
            "兑换码已兑换",
            "无效兑换码",
            "活动未开始",
            "活动已经结束",
            "领取次数过多",
    };
}