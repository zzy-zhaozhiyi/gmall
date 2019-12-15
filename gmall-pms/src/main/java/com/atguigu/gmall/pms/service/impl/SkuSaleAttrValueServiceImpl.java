package com.atguigu.gmall.pms.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.pms.dao.SkuInfoDao;
import com.atguigu.gmall.pms.dao.SkuSaleAttrValueDao;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuSaleAttrValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {
@Autowired
private SkuInfoDao skuInfoDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public List<SkuSaleAttrValueEntity> querySkuSaleAttrValueBySpuId(Long spuid) {
        //在skuinfo表中，根据spuid查询所有的skuentity
        List<SkuInfoEntity> skuInfoEntities = this.skuInfoDao.selectList(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuid));
        //获取所有的skuid ,以便下面进行查询相关的销售属性
        List<Long> skuIds = skuInfoEntities.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        //根据skuIds集合在本类中查询获得属性
        List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = this.list(new QueryWrapper<SkuSaleAttrValueEntity>().in("sku_id", skuIds));

        return skuSaleAttrValueEntities;
    }

}