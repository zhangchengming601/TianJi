package com.tianji.learning.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;


@Data
@Slf4j
public class DelayTask<G> implements Delayed {
    private G data;  // 延迟任务的数据
    private long deadlineNanos;  // 延迟时间

    public DelayTask(G data,  Duration delayTime) {
        this.data = data;
        this.deadlineNanos = System.nanoTime() + delayTime.toNanos();
    }


    // 获取延迟时间
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Math.max(0,deadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS);
    }

    // DelayQueue内部会调用compareTo方法，比较两个Delayed类型的先后执行顺序（根据延迟时间）
    @Override
    public int compareTo(Delayed o) {
        long l = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
        if(l > 0){
            return 1;
        }else if(l < 0){
            return -1;
        }else {
            return 0;
        }
    }
}
