package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoardSeason;

import java.time.LocalDate;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zcm
 * @since 2023-08-10
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {

    /**
     * 查询指定日期赛季id
     * */
    int querySeasonIdByTime(LocalDate time);

}
