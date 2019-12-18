package com.atguigu.gmall.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * 网关过滤器的实现是依赖工厂来实现的所以有配置了一个工厂配置类
 *
 * @author zzy
 * @create 2019-12-17 11:49
 */
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    //手写的这个过滤器工厂，以后那个微服务用到，直接在其下面的过滤器加上-Auth即可过滤
    @Autowired
    private AuthGatewayFilter gateWayFilter;

    @Override
    public GatewayFilter apply(Object config) {

        return gateWayFilter;
    }
}
