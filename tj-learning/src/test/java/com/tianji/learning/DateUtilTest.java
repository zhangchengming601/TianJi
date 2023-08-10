package com.tianji.learning;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
public class DateUtilTest {

    @Test
    public void test(){
//        String now = DateUtil.now();
//        DateTime time = DateUtil.parse(now, "yyyy-MM");
//        System.out.println(time.toString());

        DateTime date = DateUtil.date();
        int year = DateUtil.year(date);
        int month = DateUtil.month(date);
        int day = DateUtil.dayOfMonth(date);
        System.out.println(String.valueOf(year)+String.valueOf(month));
        System.out.println("day="+day);

        System.out.println("------------");
        System.out.println(LocalDate.now());
    }
}
