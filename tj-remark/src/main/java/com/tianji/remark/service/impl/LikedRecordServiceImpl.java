package com.tianji.remark.service.impl;


import cn.hutool.core.util.StrUtil;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
 * <p>
 * 鐐硅禐璁板綍琛 服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-08-08
 */
//@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {


    private final RabbitMqHelper mqHelper;

    /**
     * 点赞或取消点赞
     * */
    @Override
    public void addLikeRecord(LikeRecordFormDTO dto) {
        // 1. 获取当前登录用户
        Long userId = UserContext.getUser();

        // 2. 判断是否点赞
        boolean flag = dto.getLiked() ? liked(dto,userId) : unLiked(dto,userId);
        if(!flag){
            return;
        }
        // 3. 统计业务id的总点赞数
        Integer totalLikesNum = this.lambdaQuery()
                .eq(LikedRecord::getBizId, dto.getBizId())
                .count();

        // 4. 将业务总点赞数发送给MQ
        String routingKey = StrUtil.format(LIKED_TIMES_KEY_TEMPLATE, dto.getBizType());  // routingKey
        LikedTimesDTO msg = new LikedTimesDTO(dto.getBizId(), totalLikesNum);  // 消息
        // 4.1 借助RabbitMqHelper向消息队列发送消息
        mqHelper.send(LIKE_RECORD_EXCHANGE,
                routingKey,
                msg);

    }


    private boolean liked(LikeRecordFormDTO dto , Long userId){
        LikedRecord record = this.lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, dto.getBizId())
                .one();
        if (record == null){
            LikedRecord like = new LikedRecord();
            like.setUserId(userId);
            like.setBizId(dto.getBizId());
            like.setBizType(dto.getBizType());

            return this.save(like);
        }

        return false;
    }

    private boolean unLiked(LikeRecordFormDTO dto ,  Long userId){
        LikedRecord record = this.lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, dto.getBizId())
                .one();
        if (record == null){
            return false;
        }

        return this.removeById(record.getId());
    }


    /**
     * 查询当前用户是否点赞了指定的业务
     * */
    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        // 1. 获得当前用户id
        Long userId = UserContext.getUser();

        // 2. 查表，向返回值中添加数据
        // 2.查询点赞状态
        List<LikedRecord> list = lambdaQuery()
                .in(LikedRecord::getBizId, bizIds)
                .eq(LikedRecord::getUserId, userId)
                .list();
        // 3.返回结果
        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
    }

}
