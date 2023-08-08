package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.entity.InteractionQuestion;
import com.tianji.learning.entity.InteractionReply;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-08-07
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {


    private final IInteractionQuestionService questionService;
    private final UserClient userClient;


    /**
     * 新增回答或者评论
     * */
    @Override
    public void saveReply(ReplyDTO replyDTO) {

        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.新增回答
        InteractionReply reply = BeanUtils.toBean(replyDTO, InteractionReply.class);
        reply.setUserId(userId);
        save(reply);
        // 3.累加评论数或者累加回答数
        // 3.1.判断当前回复的类型是否是回答
        boolean isAnswer = replyDTO.getAnswerId() == null;
        if (!isAnswer) {
            // 3.2.是评论，则需要更新上级回答的评论数量
            lambdaUpdate()
                    .setSql("reply_times = reply_times + 1")
                    .eq(InteractionReply::getId, replyDTO.getAnswerId())
                    .update();
        }
        // 3.3.尝试更新问题表中的状态、 最近一次回答、回答数量
        questionService.lambdaUpdate()
                .set(isAnswer, InteractionQuestion::getLatestAnswerId, reply.getAnswerId())
                .setSql(isAnswer, "answer_times = answer_times + 1")
                .set(replyDTO.getIsStudent(), InteractionQuestion::getStatus, QuestionStatus.UN_CHECK.getValue())
                .eq(InteractionQuestion::getId, replyDTO.getQuestionId())
                .update();
    }


    /**
     * 分页查询回答或者评论列表
     * */
    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query) {

        // 1. 先根据条件查询reply表中的数据，questionId和answerId(至少一个不为空)
        Page<InteractionReply> page = this.lambdaQuery()
                .eq(query.getQuestionId() != null, InteractionReply::getQuestionId, query.getQuestionId())
                .eq(query.getQuestionId() != null, InteractionReply::getAnswerId, 0l)
                .eq(query.getAnswerId() != null, InteractionReply::getAnswerId, query.getAnswerId())
                .eq(InteractionReply::getHidden,false)
                .page(query.toMpPage("create_time", false));

        List<InteractionReply> records = page.getRecords();
        if (CollUtil.isEmpty(records)){
            return PageDTO.empty(page);
        }

        // 2. 查询reply中单条数据的回复者信息 （条件：非匿名）
        List<Long> ids = records.stream()
                .filter(interactionReply -> interactionReply.getAnonymity() == false)
                .map(interactionReply -> interactionReply.getUserId())
                .collect(Collectors.toList());
        // 2.1 通过fegin调用远程服务，查询User信息
        List<UserDTO> userDTOS = userClient.queryUserByIds(ids);
        // 2.2 将userDTOS转为Map数据结构
        Map<Long, UserDTO> userDTOMap = userDTOS.stream()
                .filter(userDTO -> userDTO != null)
                .collect(Collectors.toMap(userDTO -> userDTO.getId(), Function.identity()));

        // 3. 封装数据
        ArrayList<ReplyVO> replyVOS = new ArrayList<>();
        for (InteractionReply reply : records){
            // 3.1 po转vo
            ReplyVO vo = BeanUtil.copyProperties(reply, ReplyVO.class);

            // 3.2 封装reply的用户信息
            UserDTO userDTO = userDTOMap.get(reply.getUserId());
            if (userDTO != null){
                vo.setUserName(userDTO.getName());
                vo.setUserIcon(userDTO.getIcon());
                vo.setUserType(userDTO.getType());
            }
            replyVOS.add(vo);
        }


        return new PageDTO<>(page.getTotal(),page.getPages(),replyVOS);
    }
}
