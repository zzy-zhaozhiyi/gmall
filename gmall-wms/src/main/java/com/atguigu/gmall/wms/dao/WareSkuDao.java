package com.atguigu.gmall.wms.dao;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author zhaozhiyi
 * @email 962815967@qq.com
 * @date 2019-12-02 19:07:07
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void unLockStore(@Param("wareSkuId") Long wareSkuId, @Param("count") Integer count);

    List<WareSkuEntity> checkStore(@Param("skuId") Long skuId, @Param("count") Integer count);

    void lockStore(@Param("id") Long id, @Param("count") Integer count);
}
