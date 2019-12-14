package com.atguigu.gmall.index.service;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-13 18:28
 */
public interface IndexService {
    List<CategoryEntity> querylevel1Categories();

    List<CategoryVo> querySubCategery(Long pid);
}
