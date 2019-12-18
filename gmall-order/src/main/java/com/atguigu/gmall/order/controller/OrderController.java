package com.atguigu.gmall.order.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zzy
 * @create 2019-12-18 19:36
 */
@RestController
@RequestMapping("/order")
public class OrderController {
@Autowired
private OrderService orderService;

    @ApiOperation("订单进行提交")
    @GetMapping("/confirm")
    public Resp<OrderConfirmVO> confirm(){
     OrderConfirmVO confirmVO= this.orderService.confirm();//不用传参，在拦截器得到userinfo，得到userID,在redis 获取被选中的cart
     return  Resp.ok(confirmVO);
    }

}
