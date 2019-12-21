package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVO {
//虽然这里的字段和购物车的差不多，但是为了保证数据的实时性，还是要在数据中再查
    private Long skuId;

    private String title;

    private String defaultImage;

    private BigDecimal price; // 数据库价格

    private Integer count;

    private Boolean store;

    private List<SkuSaleAttrValueEntity> saleAttrValues;

    private List<SaleVO> sales;

    private BigDecimal weight;
}
