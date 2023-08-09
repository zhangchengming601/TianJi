package com.tianji.remark.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;
import static com.tianji.remark.contants.RedisConstants.LIKE_BIZ_KEY_PREFIX;
import static com.tianji.remark.contants.RedisConstants.LIKE_COUNT_KEY_PREFIX;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeRecordServiceRedisImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RedisTemplate<String,Object> redisTemplate;
    private final RabbitMqHelper mqHelper;

    /**
     * 点赞或取消点赞
     * */
    @Override
    public void addLikeRecord(LikeRecordFormDTO dto) {
        log.info("进入点赞功能");
        Long userId = UserContext.getUser();
        boolean flag = dto.getLiked() ? liked(dto,userId) : unLiked(dto,userId);
        if (!flag){
            return;
        }
        //TODO: 统计点赞数量；缓存点赞数量到redis中
        String bizType = dto.getBizType(); // 业务类型
        Long bizId = dto.getBizId();  // 业务id

        // 获得该业务id（key）下value的个数（也就是点赞数）
        String key = LIKE_BIZ_KEY_PREFIX + bizId.toString();
        Long times = redisTemplate.opsForSet().size(key);

        // 保存点赞数到redis中
        String timesKey = LIKE_COUNT_KEY_PREFIX + bizType;
        Boolean success = redisTemplate.opsForZSet().add(timesKey, bizId.toString(), times);

        log.info("保存点赞数成功，result={}",success);


    }
    private boolean liked(LikeRecordFormDTO dto , Long userId){
        String key = LIKE_BIZ_KEY_PREFIX + dto.getBizId();
        Long result = redisTemplate.opsForSet().add(key, userId.toString());
        log.info("点赞成功，result={}",result);

        return result != null && result > 0;
    }

    private boolean unLiked(LikeRecordFormDTO dto ,  Long userId){
        String key = LIKE_BIZ_KEY_PREFIX + dto.getBizId();

        Long result = redisTemplate.opsForSet().remove(key, userId.toString());
        log.info("取消点赞成功，result={}",result);
        return result != null && result > 0;
    }




    /**
     * 查询当前用户是否点赞了指定的业务
     * */
    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        // 1.获取登录用户id
        Long userId = UserContext.getUser();
        // 2.查询点赞状态
        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for (Long bizId : bizIds) {
                String key = LIKE_BIZ_KEY_PREFIX + bizId;
                src.sIsMember(key, userId.toString());
            }
            return null;
        });
        // 3.返回结果
        return IntStream.range(0, objects.size()) // 创建从0到集合size的流
                .filter(i -> (boolean) objects.get(i)) // 遍历每个元素，保留结果为true的角标i
                .mapToObj(bizIds::get)// 用角标i取bizIds中的对应数据，就是点赞过的id
                .collect(Collectors.toSet());// 收集
    }


    /**
     * 获得某业务的总点赞数，并向ma中发送该消息
     * */
    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {

        // 1. 拼接redis中的Key
        String key = LIKE_COUNT_KEY_PREFIX + bizType;

        // 2. 取数据
        ArrayList<LikedTimesDTO> dtos = new ArrayList<>();
        // 只取出并删除SortedSet中得分最少的maxBizSize个元素
        Set<ZSetOperations.TypedTuple<Object>> tupleSet = redisTemplate.opsForZSet().popMin(key, maxBizSize);
        for(ZSetOperations.TypedTuple<Object> tuple : tupleSet){
            Long value = (Long) tuple.getValue();
            Double score = tuple.getScore();
            if(value==null || score==null){
                continue;
            }
            LikedTimesDTO likedTimesDTO = new LikedTimesDTO(value, score.intValue());
            dtos.add(likedTimesDTO);
        }

        // 3. 处理数据
        mqHelper.send(
                LIKE_RECORD_EXCHANGE,
                StrUtil.format(LIKED_TIMES_KEY_TEMPLATE,bizType),
                dtos
        );


    }
}
