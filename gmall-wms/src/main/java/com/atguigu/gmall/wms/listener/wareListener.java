package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.AppConstant;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-21 19:47
 */
@Component
public class wareListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuDao wareSkuDao;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS-UNLOCK-QUEUE", durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unlockListener(String orderToken) {
//创建订单失败，和订单关单都需要进行关单，用的都是同一个监听器，为了避免两次回滚，进行判断
        String lockJson = this.redisTemplate.opsForValue().get(AppConstant.STOCK_PREFIX + orderToken);
        if (StringUtils.isEmpty(lockJson)) {
            return;
        }
        List<SkuLockVO> skuLockVOS = JSON.parseArray(lockJson, SkuLockVO.class);
        skuLockVOS.forEach(skuLockVO -> {
            this.wareSkuDao.unLockStore(skuLockVO.getWareSkuId(), skuLockVO.getCount());
        });
        this.redisTemplate.delete(AppConstant.STOCK_PREFIX + orderToken);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS-MINUS-QUEUE", durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    //订单付款成功，真正的开始减库存
    public void minusStoreListener(String orderToken) {
        String lockJson = this.redisTemplate.opsForValue().get(AppConstant.STOCK_PREFIX + orderToken);
        List<SkuLockVO> skuLockVOS = JSON.parseArray(lockJson, SkuLockVO.class);
        skuLockVOS.forEach(skuLockVO -> {
            this.wareSkuDao.minusStore(skuLockVO.getWareSkuId(), skuLockVO.getCount());
        });
    }

}
