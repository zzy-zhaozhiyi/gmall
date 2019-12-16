package com.atguigu.gmall.auth.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.exception.MemberException;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.gmall.auth.conift.JwtProperties;
import com.atguigu.gmall.auth.service.AuthService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author zzy
 * @create 2019-12-16 21:19
 */
@RequestMapping("auth")
@RestController
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtProperties properties;

    @ApiOperation("处理登录名字密码的处理，以及生成jwt")
    @PostMapping("accredit")
    public Resp<Object> accreidt(@RequestParam("username") String username, @RequestParam("password") String password,
                                 HttpServletRequest request, HttpServletResponse response) {
        String token = this.authService.accredit(username, password);
        if (StringUtils.isNotBlank(token)) {
            CookieUtils.setCookie(request, response, this.properties.getCookieName(), token, this.properties.getExpire() * 60);
            return Resp.ok(null);
        }
        throw new MemberException("用户名或者密码错误");
    }

}
