package com.atguigu.gmall.gateway.config;

import com.atguigu.core.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关过滤器的实现是依赖工厂来实现的所以有配置了一个工厂配置类
 *
 * @author zzy
 * @create 2019-12-17 11:49
 */
@Component //因为下面注入了配置文件，放在了容器中，所以下面这个类也要放在容器中，才能注入，不能一个在里面一个在外面
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilter implements GatewayFilter {

    @Autowired
    private JwtProperties properties;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //得到所有的cookies，并进行判断
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if (CollectionUtils.isEmpty(cookies)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);//设置响应状态码
            //并进行拦截，返回
            return response.setComplete();
        }
        //根据cookies的名字，来找到指定的cookie ，名字在配置文件中，就需要从中读取
        HttpCookie cookie = cookies.getFirst(this.properties.getCookieName());//这里就一个，所以就用first这个方法，得到后在进行判空
        if (cookie == null) {
            //设置状态码，进行拦截
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();

        }

        //进行解析jwt,正常进行放行，失败拦截
        try {
            JwtUtils.getInfoFromToken(cookie.getValue(), this.properties.getPublicKey());
        } catch (Exception e) {
            //进行拦截
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }


        return chain.filter(exchange);
    }
}
