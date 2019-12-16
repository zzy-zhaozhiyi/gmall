package com.atguigu.gmall.ums.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.ums.entity.MemberEntity;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author zzy
 * @create 2019-12-16 20:25
 */

public interface GmallUmsApi {

    @ApiOperation("根据用户名和密码查询用户")
    @GetMapping("ums/member/query")
    public Resp<MemberEntity> queryMemberByNameAndPassword(@RequestParam("username")String username, @RequestParam("password") String password);

}
