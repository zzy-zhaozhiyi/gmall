package com.atguigu.gmall.pms.service;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.vo.AttrGroupVO;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


/**
 * 属性分组
 *
 * @author zhaozhiyi
 * @email 962815967@qq.com
 * @date 2019-12-02 18:52:56
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {
//进行分页的操作
    PageVo queryPage(QueryCondition params);

    PageVo queryByCidPage(Long cid, QueryCondition condition);

    AttrGroupVO queryById(Long gid);

    List<AttrGroupVO> queryAttrGroupVoByCatId(Long catId);

    List<ItemGroupVO> queryItemGroupVoBySpuIdAndcatId(Long spuid, Long catid);
}

