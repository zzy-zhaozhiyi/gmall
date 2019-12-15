package com.atguigu.gmall.sms.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-05 14:58
 */
public interface GmallSmsApi {
    @PostMapping("sms/skubounds/sku/sale/save")
    public Resp<Object> saveSkuSaleVo(@RequestBody SkuSaleVo skuSaleVo);


    @ApiOperation(("根据skuid查询3个营销信息"))
    @GetMapping("sms/skubounds/{skuid}")
    public Resp<List<SaleVO>> querySalesVoBySkuId(@PathVariable("skuid") Long skuid);
}
