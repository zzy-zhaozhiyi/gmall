package com.atguiug.gmall.cart.controller;/**
 * @author zzy
 * @create 2020-06-15 19:36
 */

import org.springframework.cache.annotation.Cacheable;

/**
 *@Author 赵志毅
 *@Since 2020/6/15
 **/
public class redisTest {
    @Cacheable(value = "aaa",key="b")
    public  void get(){
    }
    
}
