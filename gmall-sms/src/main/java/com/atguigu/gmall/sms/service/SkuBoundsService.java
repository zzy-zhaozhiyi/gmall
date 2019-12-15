package com.atguigu.gmall.sms.service;

import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 商品sku积分设置
 *
 * @author zhaozhiyi
 * @email 962815967@qq.com
 * @date 2019-12-02 18:56:56
 */
public interface SkuBoundsService extends IService<SkuBoundsEntity> {

    PageVo queryPage(QueryCondition params);

    void saveSkuSaleVo(SkuSaleVo skuSaleVo);

    List<SaleVO> querySalesVoBySkuId(Long skuid);
}

