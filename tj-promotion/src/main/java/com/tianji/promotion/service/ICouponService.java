package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author zcm
 * @since 2023-08-15
 */
public interface ICouponService extends IService<Coupon> {

    /**
     * 新增优惠券接口
     * */
    void saveCoupon(CouponFormDTO dto);

    /**
     * 分页查询优惠券接口
     * */
    PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query);


    /**
     * 发放优惠券接口
     * */
    void beginIssue(CouponIssueFormDTO dto);

    /**
     * 查询发放中的优惠券
     * */
    List<CouponVO> queryIssuingCoupons();


    /**
     * 暂停发放优惠券
     * */
    void pauseIssue(Long id);
}
