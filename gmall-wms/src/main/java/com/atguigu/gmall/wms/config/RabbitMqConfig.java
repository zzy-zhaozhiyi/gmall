package com.atguigu.gmall.wms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class RabbitMqConfig {

    @Bean("WMS-TTL-QUEUE")
    public Queue ttlQueue() {
        Map<String, Object> map = new HashMap<>();
        map.put("x-dead-letter-exchange", "GMALL-ORDER-EXCHANGE");
        map.put("x-dead-letter-routing-key", "stock.unlock");
        map.put("x-message-ttl", 900000);
        return new Queue("WMS-TTL-QUEUE", true, false, false, map);
    }

    @Bean("WMS-TTL-BINDING")
    public Binding ttlBinding() {

        return new Binding("WMS-TTL-QUEUE", Binding.DestinationType.QUEUE, "GMALL-ORDER-EXCHANGE", "stock.ttl", null);
    }
//这里的死信队列，由warelistner里的来充当，所以就没写
// @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = "WMS-UNLOCK-QUEUE", durable = "true"),
//            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
//            key = {"stock.unlock"}//


}
