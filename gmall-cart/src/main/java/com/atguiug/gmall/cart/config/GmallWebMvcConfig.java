package com.atguiug.gmall.cart.config;

import com.atguiug.gmall.cart.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author zzy
 * @create 2019-12-17 19:20
 */
@Configuration
public class GmallWebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //将拦截器配置到容器中，拦截所有的路径
        registry.addInterceptor(interceptor).addPathPatterns("/**");
    }
}
