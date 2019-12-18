package com.atguiug.gmall.cart.vo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author zzy
 * @create 2019-12-17 18:14
 */
@Data
public class CartVO {

    private Long skuId;
    private String title;
    private String defaultImage;
    private BigDecimal price;//加入购物车时的价格
    private Integer count;
    private Boolean store;
    private List<SkuSaleAttrValueEntity> saleAttrValues;
    private List<SaleVO> sales;
    private Boolean check;
    private BigDecimal currentPrice;//这个是数据库的实时价格变动，实现的方法是：缓存+最终一致性（消息队列）

}
