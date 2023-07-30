package com.tianji.learning.mapper;

import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.learning.entity.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 Mapper 接口
 * </p>
 *
 * @author zcm
 * @since 2023-07-29
 */
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {


    /**
     * 查询本周每个lesson_id已学习的课程数量
     * */
    List<IdAndNumDTO> countLearnedSections(
            @Param("userId") Long userId,
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end
            );

    /**
     *查询每个lesson_id已学习的课程数量(不是本周)
     */
    List<IdAndNumDTO> countAllLearnedSections(
            @Param("userId") Long userId
    );

}
