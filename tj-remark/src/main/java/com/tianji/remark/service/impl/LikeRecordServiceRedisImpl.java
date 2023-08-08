package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.tianji.remark.contants.RedisConstants.LIKE_BIZ_KEY_PREFIX;

@Service
@RequiredArgsConstructor
public class LikeRecordServiceRedisImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RedisTemplate redisTemplate;

    /**
     * 点赞或取消点赞
     * */
    @Override
    public void addLikeRecord(LikeRecordFormDTO dto) {
        Long userId = UserContext.getUser();
        boolean flag = dto.getLiked() ? liked(dto,userId) : unLiked(dto,userId);
        //TODO: 统计点赞数量；缓存点赞数量到redis中

    }
    private boolean liked(LikeRecordFormDTO dto , Long userId){
        String key = LIKE_BIZ_KEY_PREFIX + dto.getBizId();
        Long result = redisTemplate.opsForSet().add(key, userId.toString());

        return result != null && result > 0;
    }

    private boolean unLiked(LikeRecordFormDTO dto ,  Long userId){
        String key = LIKE_BIZ_KEY_PREFIX + dto.getBizId();

        Long result = redisTemplate.opsForSet().remove(key, userId.toString());
        return result != null && result > 0;
    }




    /**
     * 查询当前用户是否点赞了指定的业务
     * */
    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        return null;
    }
}
