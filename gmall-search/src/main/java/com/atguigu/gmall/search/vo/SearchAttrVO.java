package com.atguigu.gmall.search.vo;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author zzy
 * @create 2019-12-09 18:29
 */
@Data
public class SearchAttrVO {
    @Field(type = FieldType.Long)
    private Long attrId;  //商品和属性关联的数据表的主键id

    @Field(type = FieldType.Keyword)
    private String attrName;//属性名  电池

    @Field(type =FieldType.Keyword)
    private String attrValue;//3G   3000mah
}
