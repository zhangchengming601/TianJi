package com.tianji.remark.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 鐐硅禐璁板綍琛 服务类
 * </p>
 *
 * @author zcm
 * @since 2023-08-08
 */
public interface ILikedRecordService extends IService<LikedRecord> {

    /**
     * 点赞或取消点赞
     * */
    void addLikeRecord(LikeRecordFormDTO recordFormDTO);


    /**
     * 查询当前用户是否点赞了指定的业务
     * */
    Set<Long> isBizLiked(List<Long> bizIds);
}
