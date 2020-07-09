package com.atguigu.gmall.search;/**
 * @author zzy
 * @create 2020-06-06 14:27
 */

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *@Author 赵志毅
 *@Since 2020/6/6
 **/
@SpringBootTest
public class test1 {
    @Test
    public void test2(){
        List<String> s = new ArrayList<>();
        s.add("a");
        s.add("b");
        s.add("c");
        s.add("d");
        s.add("e");
       s= s.stream().map(ss->ss.replace(" ","")).collect(Collectors.toList());
        System.out.println(s);
    }
}
