package com.atguigu.gmall.auth;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.core.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {
	private static final String pubKeyPath = "D:\\workspace_idea0722\\rsa\\rsa.pub";

    private static final String priKeyPath = "D:\\workspace_idea0722\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "23agaerrb4");
    }

   @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "nihao");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJuaWhhbyIsImV4cCI6MTU3NjUwMTU1Nn0.lG7o8J4189CL2tgqiAnlij-v2-xD21nDsds_Ki64IOWUSQarauqbaXpisc84_2JapDfAU15CCWAqVgzom1DmI9tB8bA7Lod4ggL64LGi4mkpqsE6cbqsZg7cuVgIZ0OXNZW7yWYIZnw4I3pWVsEX9Ce_g5KBLQrUXH8DLaW-xldeK6z0_jKdXLLuRIuIsSnTRhWmxl2SBOY6wbVHy2w8Mg9JVHOqKJOY9dCzgPWPvxUDy8inU8HSzofx4zDrsT-VKMbx6BpUY8JLqghdPSl_DwckVgqzJb6oA63R-ELNvobf-ASmtBNSsc9kcffx16EJc5Ejmy2_qzzYuvaxEJDeKA";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}