package com.atguigu.gmall.search.vo;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

/**
 * @author zzy
 * @create 2019-12-09 18:13
 */
@Data
@Document(indexName = "goods", type = "info", shards = 3, replicas = 2)
public class GoodsVO {
    @Id
    private Long skuId;  //skuId

    @Field(type = FieldType.Keyword, index = false)
    private String pic; //sku的默认图片

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    @Field(type = FieldType.Double)
    private Double price;//sku-price；

    @Field(type = FieldType.Long)
    private Long sale;//sku-sale 销量

    @Field(type = FieldType.Date)
    private Date creatTime;

    @Field(type = FieldType.Long)
    private Long brandId; //品牌id

    @Field(type = FieldType.Keyword)
    private String brandName;  //品牌名

    @Field(type = FieldType.Long)
    private Long categoryId;  //sku的分类id

    @Field(type = FieldType.Keyword)
    private String categoryName; //sku的名字

    @Field(type = FieldType.Boolean)
    private Boolean store;//sku-stock 库存

    //保存当前sku所有需要检索的属性；
    //检索属性来源于spu的基本属性中的search_type=1（销售属性都已经拼接在标题中了）
    @Field(type = FieldType.Nested)
    private List<SearchAttrVO> attrs;//检索属性
}

