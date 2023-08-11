package com.tianji.learning.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.tianji.learning.contants.RedisContants.POINTS_BOARD_KEY_PREFIX;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-08-10
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {


    private final StringRedisTemplate redisTemplate;


    /**
     * 1. 添加积分到数据库中，支持多种积分奖励项目（课程学习，每日签到。。。）
     * */
    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType type) {
        // 1. 判断该type每日积分是否有上限
        if (type.getMaxPoints()!=0){
            // 2. 如果有上限，则需要先获得该type今日已获得的积分总数
            int count = queryUserPointsByTypeAndDate(userId, type);
            // 2.3 如果积分没有达到上限，则保存到数据库
            if (count+points<=type.getMaxPoints()){
                Boolean success = savePointRecordToDB(userId, type, points);
                if(!success){
                    throw new BadRequestException("保存数据失败");
                }
            }else {
                throw new BadRequestException("今日积分已达上限");
            }
        }
        // 3. 如果没有上限，则直接保存到数据库
        savePointRecordToDB(userId,type,points);

        // 4. 将该用户新增加的积分添加到redis中
        String key = POINTS_BOARD_KEY_PREFIX + DateUtil.format(DateUtil.date(),"yyyyMM");
        redisTemplate.opsForZSet().incrementScore(key,userId.toString(),points);
    }

    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        // 1.获取用户
        Long userId = UserContext.getUser();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        // 3.构建查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .between(PointsRecord::getCreateTime, begin, end);
        // 4.查询
        List<PointsRecord> list = getBaseMapper().queryUserPointsByDate(wrapper);
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 5.封装返回
        List<PointsStatisticsVO> vos = new ArrayList<>(list.size());
        for (PointsRecord p : list) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }

    public Boolean savePointRecordToDB(Long userId , PointsRecordType type,int points){
        PointsRecord pointsRecord = new PointsRecord();
        pointsRecord.setPoints(points);
        pointsRecord.setType(type);
        pointsRecord.setUserId(userId);
        return this.save(pointsRecord);
    }

    private int queryUserPointsByTypeAndDate(Long userId,PointsRecordType type){
        // 2.1 获得今日日期
        DateTime date = DateUtil.date();
        DateTime begin = DateUtil.beginOfDay(date);
        DateTime end = DateUtil.endOfDay(date);

        // 2.2 查询该type今日获得的积分总数（条件：userId,，type,当天日期）
        List<PointsRecord> records = this.lambdaQuery()
                .eq(PointsRecord::getUserId, userId)
                .eq(PointsRecord::getType, type)
                .between(PointsRecord::getCreateTime, begin.toLocalDateTime(), end.toLocalDateTime())
                .list();
        if (CollUtils.isEmpty(records)){
            return 0;
        }
        // 2.3 对points字段进行求和
        Integer count = records.stream()
                .map(pointsRecord -> pointsRecord.getPoints())
                .reduce(0, (result, element) -> result + element);
        return count;
    }
}
