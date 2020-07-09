package com.atguigu.gmall.sms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest
class GmallSmsApplicationTests {

    @Test
    void contextLoads() {

        ArrayList<Integer> i = new ArrayList<>();
        i.add(11);
        System.out.println(i.get(0));

    }

}
