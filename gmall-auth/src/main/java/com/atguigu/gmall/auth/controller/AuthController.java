package com.atguigu.gmall.auth.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.exception.MemberException;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.service.AuthService;
import com.atguigu.gmall.auth.config.ScwUserAppUtils;
import com.atguigu.gmall.auth.config.SmsTemplate;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author zzy
 * @create 2019-12-16 21:19
 */
@EnableConfigurationProperties(SmsTemplate.class)
@RequestMapping("auth")
@RestController
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtProperties properties;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SmsTemplate smsTemplate;



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

    //处理发送短信验证码请求
    @ApiOperation(value = "注册时获取验证码的方法")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "phoneNum", value = "手机号码")
    })
    @GetMapping("/sendSms")
    public Resp<Object> sendSms(@RequestParam("phoneNum") String phoneNum) {
        //1、判断手机号码是否正确
        boolean flag = ScwUserAppUtils.isPhone(phoneNum);
        if (!flag) {
            return Resp.fail("你输入的手机的格式不正确");
        }
        //2、判断手机号码24小时内申请次数是否超过3次[使用redis保存，第一次访问时没有次数]
        // 拼接 手机号码 保存对应次数 存在redis中的键
        String phoneCountKey = "phone:code:" + phoneNum + ":count";
        flag = stringRedisTemplate.hasKey(phoneCountKey);//falg=true 代表之前获取过验证码
        int count = 0;
        if (flag) {
            //之前获取过验证码
            String str = stringRedisTemplate.opsForValue().get(phoneCountKey);
            count = Integer.parseInt(str);
            if (count >= 3) {
                return Resp.fail("您今天获取的验证码已经够多了，明日再来");
            }
        }
        //3、判断手机号码是否存在未使用的验证码
        //拼接该手机号码存储验证码的key
        String phoneCodeKey = "phone:code:" + phoneNum + ":code";
        flag = stringRedisTemplate.hasKey(phoneCodeKey);
        if (flag) {
            //存在未使用的验证码
            return Resp.fail("您的手机还有未使用的验证码，请先使用");
        }
        //4、生成6位验证码
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        //5、发送
        flag = smsTemplate.sendSms(phoneNum, code, "TP1711063");
        if (!flag) {
            //短信发送失败
            return Resp.fail("系统有点小故障，麻烦你稍等一会在申请");
        }
        //6、发送成功 需要将手机号码和对应的验证码保存10分钟
        stringRedisTemplate.opsForValue().set(phoneCodeKey, code, 15, TimeUnit.MINUTES);
        //7、更新当前手机号码24小时内获取验证码的次数
        if (count == 0) {
            //第一次记录次数
            stringRedisTemplate.opsForValue().set(phoneCountKey, "1", 24, TimeUnit.HOURS);
        } else {
            count++;
            //覆盖了之前的时间....
            stringRedisTemplate.opsForValue().increment(phoneCountKey);//在之前值基础上自增不会覆盖
        }
        //8、给出成功响应
        return Resp.ok("验证码发送成功");

    }


}
