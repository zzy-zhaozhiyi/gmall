package com.atguiug.gmall.cart.controller;

import com.atguigu.core.bean.Resp;
import com.atguiug.gmall.cart.service.CartService;
import com.atguiug.gmall.cart.vo.CartVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-17 18:07
 */
@RequestMapping("cart")
@RestController
public class CartController {

    @Autowired
    private CartService cartService;

    @ApiOperation(("添加购物车"))
    @PostMapping
    public Resp<Object> addCart(@RequestBody CartVO cartvo) {
        this.cartService.addCart(cartvo);
        return Resp.ok(null);
    }

    @ApiOperation("查询购物车")
    @GetMapping
    public Resp<List<CartVO>> queryCart() {//不需要什么参数，登录是用id来查，未登录用userkey来到redis中查
        List<CartVO> cartVOS = this.cartService.queryCart();
        return Resp.ok(cartVOS);
    }

    @ApiOperation("跟新购物车")
    @PostMapping("/update")
    public Resp<Object> updateCart(@RequestBody CartVO cartVO) {
        this.cartService.updateCart(cartVO);
        return Resp.ok(null);

    }

    @ApiOperation("删除购物项")
    @PostMapping("{skuid}")
    public Resp<Object> deleteCart(@PathVariable("skuid") Long skuid) {
        this.cartService.deleteCart(skuid);
        return Resp.ok(null);
    }
}
