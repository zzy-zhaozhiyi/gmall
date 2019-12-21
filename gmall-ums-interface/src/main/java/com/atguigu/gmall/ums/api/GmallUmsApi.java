package com.atguigu.gmall.ums.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-16 20:25
 */

public interface GmallUmsApi {

    @ApiOperation("根据用户名和密码查询用户")
    @GetMapping("ums/member/query")
    Resp<MemberEntity> queryMemberByNameAndPassword(@RequestParam("username") String username, @RequestParam("password") String password);

    @ApiOperation("根据UserId来查询收货地址")
    @GetMapping("ums/memberreceiveaddress/{userId}")
    public Resp<List<MemberReceiveAddressEntity>> queryMemberAdressByUserId(@PathVariable("userId") Long userId);

    @ApiOperation("详情查询")
    @GetMapping("ums/member/info/{id}")
    public Resp<MemberEntity> queryMemberByUserId(@PathVariable("id") Long id);

}
