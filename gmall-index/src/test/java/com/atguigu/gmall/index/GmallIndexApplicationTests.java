package com.atguigu.gmall.index;

import com.atguigu.gmall.index.feign.GmallPmsClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallIndexApplicationTests {
@Autowired
private GmallPmsClient gmallPmsClient;
    @Test
    void contextLoads() {
    }
    @Test
    void  test(){
        System.out.println(this.gmallPmsClient.queryCategory(1, 0l));
    }
}
