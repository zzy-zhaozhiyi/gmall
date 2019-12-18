package com.atguiug.gmall.cart.listener;

import com.atguigu.core.AppConstant;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguiug.gmall.cart.feign.GmallPmsClient;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-18 11:52
 */
@Configuration
public class CartListener {
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART-ITEM-QUEUE", durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId) {
        Resp<List<SkuInfoEntity>> listResp = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuInfoEntity> skuInfoEntities = listResp.getData();
        skuInfoEntities.forEach(skuInfoEntity -> this.redisTemplate.opsForValue().set(AppConstant.PRICE_PREFIX+skuInfoEntity.getSkuId(),skuInfoEntity.getPrice().toString()));
    }
}
