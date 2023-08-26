package com.tianji.promotion.service.impl;


import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.discount.Discount;
import com.tianji.promotion.discount.DiscountStrategy;
import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.enums.DiscountType;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.IDiscountService;
import com.tianji.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountServiceImpl implements IDiscountService {

    private final UserCouponMapper userCouponMapper;
    private final ICouponScopeService scopeService;
    private final Executor discountSolutionExecutor;




    /**
     * 查询我的优惠券可用方案
     * */
    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
        Long userId = UserContext.getUser();

        // 1. 查询当前用户可用的优惠券
        // select xxx from coupon c inner join userCoupon uc on c.id=uc.couponId where  uc.userId = xxx and  优惠券状态是未使用
        List<Coupon> availableCoupons = userCouponMapper.queryMyCoupons(userId);
        if (CollUtils.isEmpty(availableCoupons)) {
            return CollUtils.emptyList();
        }

        // 2. 初步筛选，判断优惠券使用门槛是否满足
        // 2.1 计算订单中的总价
        Integer totalAmount  = orderCourses.stream().map(OrderCourseDTO::getPrice).reduce(0, (result, element) -> result + element);
        // 2.2 筛选可用券
        for (Coupon coupon : availableCoupons) {
            DiscountType type = coupon.getDiscountType();
            Discount discount = DiscountStrategy.getDiscount(type);
            boolean canUse = discount.canUse(totalAmount, coupon);
            if (!canUse) {
                availableCoupons.remove(coupon);
            }
            if (CollUtils.isEmpty(availableCoupons)) {
                return CollUtils.emptyList();
            }
        }
        // 3.排列组合出所有方案
        // 3.1.细筛（找出每一个优惠券的可适用的课程，判断这些课程的总价是否达到优惠券的使用需求）
        Map<Coupon, List<OrderCourseDTO>> availableCouponMap = findAvailableCoupon(availableCoupons, orderCourses);
        if (CollUtils.isEmpty(availableCouponMap)) {
            return CollUtils.emptyList();
        }

        // 3.2.对可用的优惠券id进行排列组合
        availableCoupons = new ArrayList<>(availableCouponMap.keySet());
        List<List<Coupon>> solutions  = PermuteUtil.permute(availableCoupons);
        // 3.3.添加单券的方案 （因为单独使用一张券也是一种方案）
        for (Coupon c : availableCoupons) {
            solutions.add(List.of(c));
        }

        // 4.计算方案的优惠明细
        List<CouponDiscountDTO> list =
                Collections.synchronizedList(new ArrayList<>(solutions.size()));
        // 4.1.定义闭锁
        CountDownLatch latch = new CountDownLatch(solutions.size());
        for (List<Coupon> solution : solutions) {
            // 4.2 异步计算
            CompletableFuture<CouponDiscountDTO> future = CompletableFuture.supplyAsync(() -> calculateSolutionDiscount(availableCouponMap, orderCourses, solution), discountSolutionExecutor);
            // 当任务完成之后，会触发future.thenAccept这个方法
            future.thenAccept(couponDiscountDTO -> {
                // 4.3 将任务结果
                list.add(couponDiscountDTO);
                latch.countDown();
            });
        }
        // 4.4.等待运算结束
        try {
            // 主线程等待，默认超时时间1s
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("优惠方案计算被中断，{}", e.getMessage());
        }
        // 5.筛选最优解
        return findBestSolution(list);
    }



    /**
     * 筛选最优解
     * */
    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
        // 1.准备Map记录最优解
        Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();  // 最大折扣
        Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();  // 最少用券
        // 2.遍历，筛选最优解
        for (CouponDiscountDTO solution : list) {
            // 2.1.计算当前方案的id组合
            String ids = solution.getIds().stream()
                    .sorted(Long::compare).map(String::valueOf).collect(Collectors.joining(","));
            // 2.2.比较用券相同时，优惠金额是否最大
            CouponDiscountDTO best = moreDiscountMap.get(ids);
            if (best != null && best.getDiscountAmount() >= solution.getDiscountAmount()) {
                // 当前方案优惠金额少，跳过
                continue;
            }
            // 如果没有进入上面的if判断，则说明best为空或者当前方案的优惠金额大于best的优惠金额
            // 2.3.比较金额相同时，用券数量是否最少
            best = lessCouponMap.get(solution.getDiscountAmount());
            int size = solution.getIds().size();  // 用券量
            if (size > 1 && best != null && best.getIds().size() <= size) {
                // 金额相同时，当前方案用券更多，放弃
                continue;
            }
            // 如果没有进上一步的if判断，则说明 金额相同时，当前方案用券更少
            // 2.4.更新最优解
            moreDiscountMap.put(ids, solution);
            lessCouponMap.put(solution.getDiscountAmount(), solution);
        }
        // 3.求交集
        Collection<CouponDiscountDTO> bestSolutions = CollUtils
                .intersection(moreDiscountMap.values(), lessCouponMap.values());
        // 4.排序，按优惠金额降序
        return bestSolutions.stream()
                .sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed())
                .collect(Collectors.toList());
    }


    /**
     * 计算单个方案的优惠详情
     * 参数：
     *  Map<Coupon, List<OrderCourseDTO>> couponMap : 优惠券和该优惠券可用课程的映射
     *  List<OrderCourseDTO> courses : 订单中的所有课程
     *  List<Coupon> solution : 当前方案的优惠券集合
     * */
    private CouponDiscountDTO calculateSolutionDiscount(Map<Coupon, List<OrderCourseDTO>> couponMap,
                                                        List<OrderCourseDTO> courses,
                                                        List<Coupon> solution) {
        // 1, 初始化DTO
        CouponDiscountDTO dto = new CouponDiscountDTO();
        // 2. 初始化折扣明细的映射
        // 2.1 key是课程id ; value是该课程已经优惠了多少钱了
        Map<Long, Integer> detailMap  = courses.stream().map(OrderCourseDTO::getId).collect(Collectors.toMap(id -> id, oc -> 0));
        // 3. 计算折扣
        for (Coupon coupon : solution) {
            // 3.1.获取优惠券限定范围对应的课程
            List<OrderCourseDTO> availableCourses  = couponMap.get(coupon);
            // 3.2.计算该优惠券对应的课程的课程总价(课程原价 - 折扣明细)
            int totalAmount  = availableCourses.stream()
                    .mapToInt(oc -> (oc.getPrice() - detailMap.get(oc.getId())))
                    .sum();
            // 3.3.判断是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (!discount.canUse(totalAmount, coupon)) {
                // 券不可用，跳过
                continue;
            }
            // 3.4.计算优惠金额
            int discountAmount  = discount.calculateDiscount(totalAmount, coupon);
            // 3.5.计算优惠明细
            calculateDiscountDetails(detailMap, availableCourses, totalAmount, discountAmount);
            // 3.6.更新DTO数据
            dto.getIds().add(coupon.getCreater());
            dto.getRules().add(discount.getRule(coupon));
            dto.setDiscountAmount(discountAmount + dto.getDiscountAmount());
        }

        return dto;

    }


    /**
     * 计算优惠明细
     * 参数：
     *      Map<Long, Integer> detailMap : 优惠明细  key：课程id   value：该课程已优惠了多少钱
     *      List<OrderCourseDTO> courses : 满足该优惠券使用的课程的集合
     *      int totalAmount : 此时当前优惠券可用的课程的总价格（已减去之前券优惠后的）
     *      int discountAmount : 优惠的金额
     * */
    private void calculateDiscountDetails(Map<Long, Integer> detailMap,
                                          List<OrderCourseDTO> courses,
                                          int totalAmount,
                                          int discountAmount) {
        int times = 0;
        int remainDiscount = discountAmount;
        for (OrderCourseDTO course : courses) {
            // 更新课程已计算数量
            times++;
            int discount = 0;
            // 判断是否是最后一个课程
            if (times == courses.size()) {
                // 是最后一个课程，总折扣金额 - 之前所有商品的折扣金额之和
                discount = remainDiscount;
            } else {
                // 计算折扣明细（课程价格在总价中占的比例，乘以总的折扣）
                discount = discountAmount * course.getPrice() / totalAmount;
                remainDiscount -= discount;
            }
            // 更新折扣明细
            detailMap.put(course.getId(), discount + detailMap.get(course.getId()));
        }
    }


    /**
     * 查询单个优惠券可以作用在那些课程上
     * 返回值： map； key：优惠券coupon ；  value：当前优惠券coupon可以给那些课程使用
     * */
    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupon(
            List<Coupon> coupons, List<OrderCourseDTO> courses) {
        Map<Coupon, List<OrderCourseDTO>> res = new HashMap<>();

        // 1. 计算每张优惠券是否对当前courses可用
        for (Coupon coupon : coupons) {
            List<OrderCourseDTO> list = courses;

            // 一张优惠券，可以作用在多个课程上，同时也可以作用在多个分类上
            if (coupon.getSpecific()) {
                // 当该优惠券限定了作用范围时
                // 1.1查询该优惠券可以用在哪些课程或者分类上
                List<CouponScope> scopes = scopeService.lambdaQuery()
                        .eq(CouponScope::getCouponId, coupon.getId())
                        .list();
                // 1.2.获取范围对应的分类id或者课程id
                Set<Long> set = scopes.stream()
                        .map(CouponScope::getBizId)
                        .collect(Collectors.toSet());

                // 1.3从List<OrderCourseDTO> courses中筛选出当前优惠券可用的课程
                List<OrderCourseDTO> courseDTOList = list.stream()
                        .filter(orderCourseDTO -> set.contains(orderCourseDTO.getId()))
                        .collect(Collectors.toList());
            }

            // 如果没有进  if (coupon.getSpecific()) 说明该优惠券 没有设定作用范围（也就是说对任何课程都可用）
            // 2. 判断该优惠券是否可以用在当前课程（List<OrderCourseDTO> list）的集合中

            // 2.1 计算课程总价格
            Integer totalPrice = list.stream().
                    map(OrderCourseDTO::getPrice)
                    .reduce(0, (result, element) -> result + element);
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            // 2.2 判断当前优惠券是否可用
            boolean canUse = discount.canUse(totalPrice, coupon);
            if (canUse) {
                res.put(coupon,list);
            }
        }
        return res;
    }

}
