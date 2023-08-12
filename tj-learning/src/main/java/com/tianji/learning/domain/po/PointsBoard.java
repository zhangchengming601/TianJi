package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 学霸天梯榜
 * </p>
 *
 * @author zcm
 * @since 2023-08-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("points_board")
@ApiModel(value="PointsBoard对象", description="学霸天梯榜")
public class PointsBoard implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "榜单id")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "学生id")
    private Long userId;

    @ApiModelProperty(value = "积分值")
    private Integer points;

}
