package com.atguiug.gmall.cart.interceptors;

import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;
import com.atguiug.gmall.cart.config.JwtProperties;
import com.atguigu.core.bean.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

/**
 * 配置这个拦截器的目的就是为了获得token和userkey信息，传递给下面的服务进行操作
 * @author zzy
 * @create 2019-12-17 19:05
 */
@EnableConfigurationProperties(JwtProperties.class)
@Component
public class LoginInterceptor extends HandlerInterceptorAdapter {//通过配置类将拦截器加入到容器中
    @Autowired
    private JwtProperties properties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();
    //另一种将值传递的方式放进request域中，进行传值，但是看着没有这个看起来优雅


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo = new UserInfo();
        // 获取cookie中的token信息（jwt）及userKey信息
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());
        //不管是不是已经登录，这个user-key都会存在
        String userKey = CookieUtils.getCookieValue(request, this.properties.getUserKey());

        // 判断有没有userKey，没有：制作一个放入cookie中
        if (StringUtils.isEmpty(userKey)) {
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, this.properties.getUserKey(), userKey, 6 * 30 * 24 * 3600);
        }
        userInfo.setUserKey(userKey);

        // 判断有没有token
        if (StringUtils.isNotBlank(token)) {
            // 解析token信息
            Map<String, Object> infoFromToken = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
            userInfo.setId(new Long(infoFromToken.get("id").toString()));
        }

        THREAD_LOCAL.set(userInfo);

        return super.preHandle(request, response, handler);
    }

    public static UserInfo getUserInfo(){

        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 必须手动清除threadLocal中线程变量，因为使用的是tomcat线程池
        THREAD_LOCAL.remove();
    }
}
