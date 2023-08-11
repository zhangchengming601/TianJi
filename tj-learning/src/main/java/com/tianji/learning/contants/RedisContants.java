package com.tianji.learning.contants;

public class RedisContants {

    /**
     * 签到记录key的前缀(sign:uid+用户id+日期) ： sign:uid:101:202308
     */
    public static String SIGN_RECORD_KEY_PREFIX = "sign:uid:";


    /**
     * 积分排行榜key的前缀 (boards:日期)： boards:202301
     * */
    public static String POINTS_BOARD_KEY_PREFIX = "boards:";
}
