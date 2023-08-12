package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author zcm
 * @since 2023-08-10
 */
public interface IPointsBoardService extends IService<PointsBoard> {

    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);


    /**
     * 创建赛季积分表
     * */
    void createPointsBoardTableBySeason(Integer season);

}
