package com.tianji.learning.handler;


import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 积分榜单持久化
 * */
@RequiredArgsConstructor
@Component
public class PointsBoardPersistentHandler {

    private final StringRedisTemplate redisTemplate;
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    private final IPointsBoardService pointsBoardService;



    @Scheduled(cron = "0 0 3 1 * ?")
    public void persistentPointBoardToDB(){
        // 1. 从redis中获得数据（上个月的积分排行榜）
        // 1.1 按照score分数，从大到小查出来

        // 2. 保存数据到mysql

        // 3. 删除redis中上个月的积分排行榜数据
    }

    /**
     * 每月1号，凌晨3点执行
     * 创建当赛季积分榜单
     * */
    @XxlJob("createTableJob")
    public void createPointsBoardTableOfLastSeason(){
        // 1. 查询上个月赛季的id
        LocalDate time = LocalDate.now().minusMonths(1);
        int seasonId = pointsBoardSeasonService.querySeasonIdByTime(time);
        if (seasonId == 0){
            return;
        }
        // 2. 创建表
        pointsBoardService.createPointsBoardTableBySeason(seasonId);
    }
}
