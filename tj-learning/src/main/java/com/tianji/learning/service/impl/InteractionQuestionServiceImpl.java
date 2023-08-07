package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.entity.InteractionQuestion;
import com.tianji.learning.entity.InteractionReply;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author zcm
 * @since 2023-08-07
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final UserClient userClient;

    private final IInteractionReplyService replyService;

    private final SearchClient searchClient;

    private final CatalogueClient catalogueClient;

    private final CourseClient courseClient;


    /**
     * 新增互动问题
     * */
    @Override
    public void saveQuestion(QuestionFormDTO questionDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.数据转换
        InteractionQuestion question = BeanUtils.toBean(questionDTO, InteractionQuestion.class);
        // 3.补充数据
        question.setUserId(userId);
        // 4.保存问题
        save(question);
    }


    /**
     * 修改互动问题
     * */
    @Override
    public void updateQuesion(Long id,QuestionFormDTO dto) {
        // 1.检验参数
        if (StringUtils.isBlank(dto.getTitle())
                || StringUtils.isBlank(dto.getDescription())
                || dto.getAnonymity() == null){
            throw new BadRequestException("非法参数");
        }
        InteractionQuestion question = this.getById(id);
        if (question == null){
            throw new BadRequestException("非法参数");
        }
        Long userId = UserContext.getUser();
        if(!userId.equals(question.getUserId())){
            throw new BadRequestException("不能修改别人的互动问题");
        }

        // 2. dto 转po
        question.setTitle(dto.getTitle());
        question.setDescription(dto.getDescription());
        question.setAnonymity(dto.getAnonymity());


        this.updateById(question);
    }


    /**
     * 用户端分页查询问题
     * */
    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {

        Long userId = UserContext.getUser();

        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("课程id和小节id不能都为空");
        }

        // 1.根据请求参数，查询question表中的数据
        Page<InteractionQuestion> page = this.lambdaQuery()
                .select(InteractionQuestion.class,i ->!i.getColumn().equals("description"))
                .eq(query.getCourseId() != null, InteractionQuestion::getCourseId, query.getCourseId())
                .eq(query.getSectionId() != null, InteractionQuestion::getSectionId, query.getSectionId())
                .eq(query.getOnlyMine() != null && query.getOnlyMine() == true, InteractionQuestion::getUserId, userId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();


        // 2. 查询每一条question数据(非匿名)的用户信息（用户id,用户头像，用户名称）
        List<Long> userIdList = records.stream()
                .filter(interactionQuestion -> interactionQuestion.getAnonymity() != true)
                .map(interactionQuestion -> interactionQuestion.getUserId())
                .collect(Collectors.toList());
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIdList);
        Map<Long, UserDTO> userDTOMap = userDTOS.stream()
                .collect(Collectors.toMap(userDTO -> userDTO.getId(), userDTO -> userDTO));

        // 3. 根据最近一个回答的id查询（回答人的名称，回答内容）
        List<Long> answIds = records.stream()
                .map(interactionQuestion -> interactionQuestion.getLatestAnswerId())
                .collect(Collectors.toList());

        Map<Long, UserDTO> answUsersMap = null;
        Map<Long, InteractionReply> repliesMap = null;
        if (CollUtil.isNotEmpty(answIds)){
            List<InteractionReply> replies = replyService.getBaseMapper().selectBatchIds(answIds);
            repliesMap = replies.stream()
                    .collect(Collectors.toMap(reply -> reply.getId(), reply -> reply));
            List<Long> answUserId = replies.stream()
                    .filter(interactionReply -> interactionReply.getAnonymity() != true)
                    .map(interactionReply -> interactionReply.getUserId())
                    .collect(Collectors.toList());
            List<UserDTO> answUsers = userClient.queryUserByIds(answUserId);
            answUsersMap = answUsers.stream()
                    .filter(userDTO -> userDTO != null)
                    .collect(Collectors.toMap(userDTO -> userDTO.getId(), userDTO -> userDTO));
        }



        // 4.数据封装
        ArrayList<QuestionVO> questionVOS = new ArrayList<>();
        for (InteractionQuestion record : records){
            //po转vo
            QuestionVO vo = BeanUtil.copyProperties(record, QuestionVO.class);

            // 封装用户信息
            UserDTO userDTO = userDTOMap.get(record.getUserId());
            if(userDTO!=null){
                vo.setUserName(userDTO.getName());
                vo.setUserIcon(userDTO.getIcon());
            }

            // 封装回答者信息
            if(CollUtil.isNotEmpty(answUsersMap)&&CollUtil.isNotEmpty(repliesMap)){
                UserDTO answUserDTO = answUsersMap.get(record.getLatestAnswerId());
                if (answUserDTO != null){
                    vo.setLatestReplyUser(answUserDTO.getName());
                    vo.setLatestReplyContent(repliesMap.get(record.getLatestAnswerId()).getContent());
                }
            }

            questionVOS.add(vo);
        }

        return new PageDTO<>(page.getTotal(),page.getPages(),questionVOS);
    }


    /**
     * 根据问题id查询问题详情
     * */
    @Override
    public QuestionVO queryQuestionById(Long id) {
        // 1.根据id查询数据
        InteractionQuestion question = getById(id);
        // 2.数据校验
        if(question == null || question.getHidden()){
            // 没有数据或者是被隐藏了
            return null;
        }
        // 3.查询提问者信息
        UserDTO user = null;
        if(!question.getAnonymity()){
            user = userClient.queryUserById(question.getUserId());
        }
        // 4.封装VO
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        return vo;
    }


    /**
     * 管理端分页查询问题
     * */
    @Override
    public PageDTO<QuestionAdminVO> queryQuestionAdminVOPage(QuestionAdminPageQuery query) {

        // 1.处理课程名称，得到课程id
        List<Long> courseIds = null;
        if (StringUtils.isNotBlank(query.getCourseName())) {
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if (CollUtils.isEmpty(courseIds)) {
                return PageDTO.empty(0L, 0L);
            }
        }

        // 2. 分页查询--查询互动问题表  条件：前端传的条件 ； 分页；  排序（提问时间倒序）
        Integer status = query.getStatus();
        LocalDateTime begin = query.getBeginTime();
        LocalDateTime end = query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(begin != null, InteractionQuestion::getCreateTime, begin)
                .lt(end != null, InteractionQuestion::getCreateTime, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        // 3.准备VO需要的数据：用户数据、课程数据、章节数据
        Set<Long> userIds = new HashSet<>();
        Set<Long> cIds = new HashSet<>();
        Set<Long> cataIds = new HashSet<>();
        // 3.1.获取各种数据的id集合
        for (InteractionQuestion q : records) {
            userIds.add(q.getUserId());
            cIds.add(q.getCourseId());
            cataIds.add(q.getChapterId());
            cataIds.add(q.getSectionId());
        }
        // 3.2.根据id查询用户
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap = new HashMap<>(users.size());
        if (CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        // 3.3.根据id查询课程
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> cInfoMap = new HashMap<>(cInfos.size());
        if (CollUtils.isNotEmpty(cInfos)) {
            cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }

        // 3.4.根据id查询章节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }




        return null;
    }
}
