package com.tianji.promotion.service;

import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;

import java.util.List;

public interface IDiscountService {

    /**
     * 查询我的优惠券可用方案
     * */
    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses);
}
