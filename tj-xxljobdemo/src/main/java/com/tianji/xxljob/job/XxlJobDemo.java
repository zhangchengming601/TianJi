package com.tianji.xxljob.job;


import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XxlJobDemo {


    @XxlJob("xxltest")
    public void test(){
        log.debug("xxxjob 任务执行了");
    }
}
