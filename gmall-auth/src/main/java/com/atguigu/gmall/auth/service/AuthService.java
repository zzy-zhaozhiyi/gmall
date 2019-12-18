package com.atguigu.gmall.auth.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.ums.entity.MemberEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * @author zzy
 * @create 2019-12-16 21:22
 */
@EnableConfigurationProperties(JwtProperties.class)
@Service
public class AuthService {
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private JwtProperties properties;

    public String accredit(String username, String password) {
        //远程调用，检验用户名和密码，并判断是否为空
        Resp<MemberEntity> memberEntityResp = this.umsClient.queryMemberByNameAndPassword(username, password);
        MemberEntity memberEntity = memberEntityResp.getData();

        System.out.println(memberEntity+"===========================这里是authService");

        if (memberEntity == null) {
            return null;
        }
        //制作jwt

        try {
            HashMap<String, Object> map = new HashMap<>();
            map.put("username", memberEntity.getUsername());
            map.put("id", memberEntity.getId());
            String token = JwtUtils.generateToken(map, this.properties.getPrivateKey(), this.properties.getExpire());
            return  token;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //放进cookie中,需要request,response.索性就将这个放在controller中实现
        return null;
    }
}
