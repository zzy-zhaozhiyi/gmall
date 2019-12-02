package com.atguigu.gmall.sms.dao;

import com.atguigu.gmall.sms.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author zhaozhiyi
 * @email 962815967@qq.com
 * @date 2019-12-02 18:56:56
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
