package com.atguigu.gmall.ums.dao;

import com.atguigu.gmall.ums.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author zhaozhiyi
 * @email 962815967@qq.com
 * @date 2019-12-02 19:02:11
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
