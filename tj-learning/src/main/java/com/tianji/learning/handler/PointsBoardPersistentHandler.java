package com.tianji.learning.handler;


import cn.hutool.core.collection.CollUtil;
import com.tianji.common.exceptions.DbException;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.tianji.learning.contants.PointsBoardContants.Dynamic_TableName_Prefix;
import static com.tianji.learning.contants.RedisContants.POINTS_BOARD_KEY_PREFIX;

/**
 * 积分榜单持久化
 * */
@RequiredArgsConstructor
@Component
public class PointsBoardPersistentHandler {

    private final StringRedisTemplate redisTemplate;
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    private final IPointsBoardService pointsBoardService;



    /**
     * 持久化上赛季（上个月）积分榜单数据到mysql中
     * */
    @XxlJob("savePointsBoard2DB")
    public void savePointBoardToDB(){

        // 设置动态表名
        LocalDate time = LocalDate.now().minusMonths(1);
        int seasonId = pointsBoardSeasonService.querySeasonIdByTime(time);
        TableInfoContext.setInfo(Dynamic_TableName_Prefix+seasonId);

        // 1. 从redis中获得数据（上个月的积分排行榜）
        String key = POINTS_BOARD_KEY_PREFIX + DateTimeFormatter.ofPattern("yyyyMM").format(time);

        // 1.1 按照score分数，从大到小查出来
        int pageNo = 1;
        int pageSize = 1000;
        while (true){
            List<PointsBoard> pointsBoards = pointsBoardService.queryCurrentBoardList(key, pageNo, pageSize);
            if (CollUtil.isEmpty(pointsBoards)){
                break;
            }
            // 2. 保存数据到mysql
            Boolean success = pointsBoardService.savePointsBoardList(pointsBoards);
            if (!success){
                throw new DbException("持久化redis中积分榜单数据错误");
            }
            // 更新分页数据
            pageNo++;
        }

        // 任务结束，移除动态表名
        TableInfoContext.remove();
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
