package com.tianji.learning;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@SpringBootTest
public class RedisBitMapTest {
    @Autowired
    StringRedisTemplate redisTemplate;


    @Test
    public void test(){
        Boolean bit = redisTemplate.opsForValue().setBit("test116", 4, true);
        System.out.println(bit);  //bit=false； 表示在修改之前，这里的值为0
    }

    @Test
    public void test2(){
        List<Long> field = redisTemplate.opsForValue().bitField("test116",
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(6)).valueAt(0));
        System.out.println(field);  // 输出的是十进制的2
    }

}
