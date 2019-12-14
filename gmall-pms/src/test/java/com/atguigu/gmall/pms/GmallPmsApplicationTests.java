package com.atguigu.gmall.pms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallPmsApplicationTests {
    @Value("${item.rabbitmq.exchange}")
    private String EXCHANGE_NAME;
    @Test
    void contextLoads() {
        System.out.println(EXCHANGE_NAME);
    }

}
