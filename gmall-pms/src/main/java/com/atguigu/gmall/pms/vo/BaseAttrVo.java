package com.atguigu.gmall.pms.vo;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.alibaba.nacos.client.naming.utils.StringUtils;
import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import lombok.Data;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-04 21:22
 */
@Data
public class BaseAttrVo extends ProductAttrValueEntity {
    public void setValueSelected(List<String> selected){

        if (CollectionUtils.isEmpty(selected)) {
            return ;
        }
        this.setAttrValue(StringUtils.join(selected, ","));
    }
}
