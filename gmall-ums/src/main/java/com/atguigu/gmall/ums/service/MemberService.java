package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * 会员
 *
 * @author zhaozhiyi
 * @email 962815967@qq.com
 * @date 2019-12-02 19:02:11
 */
public interface MemberService extends IService<MemberEntity> {

    PageVo queryPage(QueryCondition params);

    Boolean checkDataByType(String data, Integer type);

    void register(MemberEntity memberEntity, String code);

    MemberEntity queryMemberByNameAndPassword(String username, String password);
}

