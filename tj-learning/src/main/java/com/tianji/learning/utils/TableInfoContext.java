package com.tianji.learning.utils;


/**
 * 动态表名
 * */
public class TableInfoContext {
    private static final ThreadLocal<String> TL = new ThreadLocal<>();


    /**
     * String info : 表名
     * */
    public static void setInfo(String info){
        TL.set(info);
    }

    public static String getInfo(){
        return TL.get();
    }

    public static void remove(){
        TL.remove();
    }
}
