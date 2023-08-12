package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-08-10
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {


    /**
     * 查询指定日期赛季id
     * */
    @Override
    public int querySeasonIdByTime(LocalDate time) {
        PointsBoardSeason one = this.lambdaQuery()
                .gt(PointsBoardSeason::getEndTime, time)
                .lt(PointsBoardSeason::getBeginTime, time)
                .one();
        if (one !=null){
            return one.getId();
        }

        return 0;
    }
}
