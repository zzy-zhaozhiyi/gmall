package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-13 19:21
 */
@Data
@ToString
public class CategoryVo extends CategoryEntity {
    private List<CategoryEntity> subs;
}
