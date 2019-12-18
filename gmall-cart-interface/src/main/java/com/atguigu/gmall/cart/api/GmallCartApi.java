package com.atguigu.gmall.cart.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.vo.CartVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-18 19:23
 */
public interface GmallCartApi {
    @ApiOperation("根据userId来查询被选中的购物项")
    @GetMapping("cart/check/{userId}")
    public Resp<List<CartVO>> queryCheckAndCartByUserId(@PathVariable("userId") Long userId);
}
