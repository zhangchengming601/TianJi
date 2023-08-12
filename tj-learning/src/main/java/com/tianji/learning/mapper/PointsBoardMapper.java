package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domain.po.PointsBoard;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 学霸天梯榜 Mapper 接口
 * </p>
 *
 * @author zcm
 * @since 2023-08-10
 */
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {

    /**
     * 创建赛季积分表
     * */
    void createPointsBoardTable(@Param("tableName") String tableName);

}
