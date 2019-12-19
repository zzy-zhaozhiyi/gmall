package com.atguigu.gmall.wms.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private WareSkuDao wareSkuDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public String checkAndLockStore(List<SkuLockVO> skuLockVOS) {

        if (CollectionUtils.isEmpty(skuLockVOS)) {
            return "没有选中的商品";
        }

        // 1、检验并锁定库存,先通过这个对每个商品进行检查，是否满足有货的要求，有货是true,没货是false
        skuLockVOS.forEach(skuLockVO -> {
            lockStore(skuLockVO);
        });
       //2、检查后，过滤不符合有货要求的商品，并进行判空，有一个没成功锁定就全部回滚
        List<SkuLockVO> unLockSku = skuLockVOS.stream().filter(skuLockVO -> skuLockVO.getLock() == false).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(unLockSku)) {
            // 解锁已锁定商品的库存
            List<SkuLockVO> lockSku = skuLockVOS.stream().filter(SkuLockVO::getLock).collect(Collectors.toList());
            lockSku.forEach(skuLockVO -> {
                this.wareSkuDao.unLockStore(skuLockVO.getWareSkuId(), skuLockVO.getCount());
            });
            // 提示锁定失败的商品
            List<Long> skuIds = unLockSku.stream().map(SkuLockVO::getSkuId).collect(Collectors.toList());
            return "下单失败，商品库存不足：" + skuIds.toString();
        }

        return null;
    }

    private void lockStore(SkuLockVO skuLockVO) {

        RLock lock = this.redissonClient.getLock("stock:" + skuLockVO.getSkuId());
        lock.lock();
        // 查询剩余库存够不够
        List<WareSkuEntity> wareSkuEntities = this.wareSkuDao.checkStore(skuLockVO.getSkuId(), skuLockVO.getCount());
        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
            // 锁定库存信息
            Long id = wareSkuEntities.get(0).getId();
            this.wareSkuDao.lockStore(id, skuLockVO.getCount());
            skuLockVO.setWareSkuId(id);
            skuLockVO.setLock(true);
        } else {
            skuLockVO.setLock(false);
        }

        lock.unlock();
    }

}