package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

import java.util.List;

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


    /**
     * 保存上赛季榜单数据到数据库中
     * */
    Boolean savePointsBoardList(List<PointsBoard> pointsBoards);


    /**
     * 从redis中分页查询上赛季积分榜单数据
     * */
    List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize);

}
