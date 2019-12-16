package com.atguigu.gmall.ums.controller;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;


/**
 * 会员
 *
 * @author zhaozhiyi
 * @email 962815967@qq.com
 * @date 2019-12-02 19:02:11
 */
@Api(tags = "会员 管理")
@RestController
@RequestMapping("ums/member")
public class MemberController {
    @Autowired
    private MemberService memberService;
@ApiOperation("根据用户名和密码查询用户")
@GetMapping("/query")
public Resp<MemberEntity> queryMemberByNameAndPassword(@RequestParam("username")String username, @RequestParam("password") String password){
   MemberEntity memberEntity = this.memberService.queryMemberByNameAndPassword(username,password);
   return Resp.ok(memberEntity);

}

    @ApiOperation("处理登录验证")
    @GetMapping("/check/{data}/{type}")
    public Resp<Boolean> checkDataByType(@PathVariable("data") String data, @PathVariable("type") Integer type) {
        Boolean flag = this.memberService.checkDataByType(data, type);

        return Resp.ok(flag);
    }

    @ApiOperation("处理用户註冊")
    @PostMapping("/register")
    public Resp<Object> register(MemberEntity memberEntity, @RequestParam("code") String code) {

        this.memberService.register(memberEntity, code);
        return Resp.ok(null);
    }

    /**
     * 列表
     */
    @ApiOperation("分页查询(排序)")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ums:member:list')")
    public Resp<PageVo> list(QueryCondition queryCondition) {
        PageVo page = memberService.queryPage(queryCondition);

        return Resp.ok(page);
    }


    /**
     * 信息
     */
    @ApiOperation("详情查询")
    @GetMapping("/info/{id}")
    @PreAuthorize("hasAuthority('ums:member:info')")
    public Resp<MemberEntity> info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);

        return Resp.ok(member);
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('ums:member:save')")
    public Resp<Object> save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return Resp.ok(null);
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('ums:member:update')")
    public Resp<Object> update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return Resp.ok(null);
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('ums:member:delete')")
    public Resp<Object> delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return Resp.ok(null);
    }

}
