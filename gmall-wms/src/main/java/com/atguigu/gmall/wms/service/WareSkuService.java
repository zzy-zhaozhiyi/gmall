package com.atguigu.gmall.wms.service;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 商品库存
 *
 * @author zhaozhiyi
 * @email 962815967@qq.com
 * @date 2019-12-02 19:07:07
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageVo queryPage(QueryCondition params);
}

