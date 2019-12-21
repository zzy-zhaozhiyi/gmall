package com.atguigu.gmall.oms.dao;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author zhaozhiyi
 * @email 962815967@qq.com
 * @date 2019-12-02 18:40:51
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
    public int closeOrder(String orderToken);

    public int payOrder(String orderToken);

}
