package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tianji.learning.contants.LearningContants.POINTS_BOARD_TABLE_PREFIX;
import static com.tianji.learning.contants.RedisContants.POINTS_BOARD_KEY_PREFIX;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-08-10
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;
    private final IPointsBoardSeasonService pointsBoardSeasonService;

    /**
     * 分页查询指定赛季的积分排行榜
     * */
    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {

        Long userId = UserContext.getUser();
        Boolean isCurrent = query.getSeason() == null || query.getSeason() == 0;

        List<PointsBoard> pointsBoards = null;
        PointsBoard selfBoard = null;


        // 1. 判断请求参数中赛季id是否为空，为空则查询当前赛季的积分排行榜
        if (isCurrent){
            // 1.1 当前赛季的积分排行榜需要去redis中查询
            pointsBoards = queryCurrentSeasonBoard(query.getPageNo(), query.getPageSize());

            // 1.2 在redis中查询个人排名
            selfBoard = querySelfCurrentSeasonBoard(userId);
        }else{
            // 2. 参数中赛季id不为空，则去数据库中查询指定赛季的积分排行榜
            pointsBoards = queryCurrentSeasonBoardFromDB(query.getSeason(), query.getPageNo(), query.getPageSize());
            selfBoard = querySelfCurrentSeasonBoardFromDB(userId, query.getSeason());
        }

        // 3. 封装数据
        PointsBoardVO vo = new PointsBoardVO();

        // 3.1 封装个人积分排行数据
        if (selfBoard != null){
            vo.setRank(selfBoard.getId().intValue());
            vo.setPoints(selfBoard.getPoints());
        }
        // 3.2 封装当赛季积分排行榜
        if(CollUtil.isNotEmpty(pointsBoards)){
            // 3.2.1 通过fegin远程调用user微服务，查询多个user信息
            List<Long> userIds = pointsBoards.stream().map(PointsBoard::getUserId).collect(Collectors.toList());
            List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
            if (CollUtil.isEmpty(userDTOS)){
                return vo;
            }

            // 将List集合的user信息转为map集合
            Map<Long, String> userIdNaMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
            // 填充数据
            ArrayList<PointsBoardItemVO> boardItemVOS = new ArrayList<>();
            for (PointsBoard board : pointsBoards){
                PointsBoardItemVO boardItemVO = BeanUtil.copyProperties(board, PointsBoardItemVO.class);
                boardItemVO.setName(userIdNaMap.get(board.getUserId()));
                boardItemVOS.add(boardItemVO);
            }

            vo.setBoardList(boardItemVOS);
        }

        return vo;
    }

    /**
     * 创建赛季积分表
     * */
    @Override
    public void createPointsBoardTableBySeason(Integer season) {
        this.getBaseMapper().createPointsBoardTable(POINTS_BOARD_TABLE_PREFIX+season );

    }

    /**
     * 保存上赛季榜单数据到数据库中
     * */
    @Override
    public Boolean savePointsBoardList(List<PointsBoard> pointsBoards) {

        // 保存数据
        boolean success = this.saveBatch(pointsBoards);
        return success;
    }

    /**
     * 从redis中分页查询上赛季积分榜单数据
     * */
    @Override
    public List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize) {
        int start = (pageNo-1) * pageSize;
        int end = start + pageSize-1;

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        if (CollUtil.isEmpty(tuples)){
            return CollUtils.emptyList();
        }
        ArrayList<PointsBoard> pointsBoards = new ArrayList<>();

        Long rank = (long)((pageNo-1) * pageSize);
        // 2. 封装数据
        for (ZSetOperations.TypedTuple<String> tuple : tuples){
            if (tuple != null){
                String value = tuple.getValue();
                Double score = tuple.getScore();

                Long userId = Long.valueOf(value);
                int points = score.intValue();

                PointsBoard board = new PointsBoard();
                board.setId(++rank);
                board.setUserId(userId);
                board.setPoints(points);
                pointsBoards.add(board);
            }
        }
        return pointsBoards;
    }


    /**
     * 在mysql中查询历史赛季积分排行榜
     * */
    public List<PointsBoard> queryCurrentSeasonBoardFromDB(Long season , int pageNo , int pageSize){
        // 条件： 指定赛季，指定分页大小，按照points进行降序
        Page<PointsBoard> page = this.lambdaQuery()
                .page(new Page<>(pageNo, pageSize));
        List<PointsBoard> records = page.getRecords();
        return records;
    }


    /**
     * 在mysql中查询历史赛季积分个人排行
     * */
    public PointsBoard querySelfCurrentSeasonBoardFromDB(Long userId , Long season){
        PointsBoard one = this.lambdaQuery()
                .eq(PointsBoard::getUserId, userId)
                .one();
        return one;
    }


    /**
     * 在redis中查询当前赛季个人的排行
     * */
    private PointsBoard querySelfCurrentSeasonBoard(Long userId){
        // redis中的key
        String key = POINTS_BOARD_KEY_PREFIX + DateUtil.format(DateUtil.date(),"yyyyMM");

        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId.toString())+1;
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());

        PointsBoard board = new PointsBoard();
        board.setUserId(userId);
        board.setPoints(score.intValue());
        board.setId(rank);

        return board;

    }


    /**
     * 在redis中查询当前赛季的积分排行榜
     * */
    private List<PointsBoard> queryCurrentSeasonBoard(int pageNo , int pageSize){
        // redis中的key
        String key = POINTS_BOARD_KEY_PREFIX + DateUtil.format(DateUtil.date(),"yyyyMM");
        // 分页数据
        int start = (pageNo-1)*pageSize;  // 起始位置
        int end = start+pageSize-1;   // 结束位置

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);

        Long rank = start+1l;
        ArrayList<PointsBoard> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples){
            if (tuple == null){
                continue;
            }
            String value = tuple.getValue();
            Double score = tuple.getScore();
            Long userId = Long.valueOf(value);
            PointsBoard board = new PointsBoard();
            board.setUserId(userId);
            board.setPoints(score.intValue());
            board.setId(rank++);
            result.add(board);
        }

        return result;
    }
}
