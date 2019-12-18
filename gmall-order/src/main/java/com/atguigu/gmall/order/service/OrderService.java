package com.atguigu.gmall.order.service;

import com.atguigu.core.AppConstant;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.MemberException;
import com.atguigu.gmall.cart.vo.CartVO;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptors.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import com.atguigu.gmall.order.vo.OrderItemVO;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author zzy
 * @create 2019-12-18 19:36
 */
@Service
public class OrderService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private GmallCartClient cartClient;

//这个一部编排是4个外部的，到最后阻塞下。第三个大的里面还有4个，到最后在阻塞
    public OrderConfirmVO confirm() {
        OrderConfirmVO confirmVO = new OrderConfirmVO();
        //先获得拦截器中内容，并进行判断
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getId();
        if (userId == null) {
            return null;
        }

        //1、根据userId在ums DB中查询收货地址
        CompletableFuture<Void> umsCompletableFuture = CompletableFuture.runAsync(() -> {
            List<MemberReceiveAddressEntity> addressEntities = this.umsClient.queryMemberAdressByUserId(userId).getData();
            confirmVO.setAddresses(addressEntities);
        }, threadPoolExecutor);


        //2、根据userId来查询member，进而得到可用积分
        CompletableFuture<Void> memberCompletableFuture = CompletableFuture.runAsync(() -> {
            MemberEntity memberEntity = this.umsClient.queryMemberByUserId(userId).getData();
            confirmVO.setBounds(memberEntity.getIntegration());
        }, threadPoolExecutor);


        //3、查询类似订单的选项，要在查下，确保数据的实时性，不用购物车
        //远程调用不能用不能传递cookie信息，
        CompletableFuture<Void> bigSkuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            List<CartVO> cartVOS = this.cartClient.queryCheckAndCartByUserId(userId).getData();
            if (CollectionUtils.isEmpty(cartVOS)) {
                throw new MemberException("请勾选购物项");
            }
            return cartVOS;

        }, threadPoolExecutor).thenAcceptAsync(cartVOS -> {


            List<OrderItemVO> orderItemVOS = cartVOS.stream().map(cartVO -> {
                OrderItemVO orderItemVO = new OrderItemVO();
                Long skuId = cartVO.getSkuId();
                //sku相关的
                CompletableFuture<Void> skuCompletableFuture = CompletableFuture.runAsync(() -> {
                    SkuInfoEntity skuInfoEntity = this.pmsClient.querySkuInfoBySkuId(skuId).getData();
                    if (skuInfoEntity != null) {

                        orderItemVO.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
                        orderItemVO.setTitle(skuInfoEntity.getSkuTitle());
                        orderItemVO.setWeight(skuInfoEntity.getWeight());
                        orderItemVO.setSkuId(skuInfoEntity.getSkuId());
                        orderItemVO.setPrice(skuInfoEntity.getPrice());
                        orderItemVO.setCount(cartVO.getCount());
                    }
                }, threadPoolExecutor);


                //销售属性
                CompletableFuture<Void> saleAttrValueCompletableFuture = CompletableFuture.runAsync(() -> {
                    List<SkuSaleAttrValueEntity> saleAttrValueEntities = this.pmsClient.querySkuSaleAttrValueBySkuId(skuId).getData();
                    orderItemVO.setSaleAttrValues(saleAttrValueEntities);
                }, threadPoolExecutor);


                //设置买赠打折信息
                CompletableFuture<Void> salesCompletableFuture = CompletableFuture.runAsync(() -> {
                    List<SaleVO> saleVOS = this.smsClient.querySalesVoBySkuId(skuId).getData();
                    orderItemVO.setSales(saleVOS);
                }, threadPoolExecutor);

                //库存信息
                CompletableFuture<Void> wareCompletableFuture = CompletableFuture.runAsync(() -> {
                    List<WareSkuEntity> wareSkuEntities = this.wmsClient.queryWareSkuBySkuId(skuId).getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        boolean flag = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                        orderItemVO.setStore(flag);
                    }
                }, threadPoolExecutor);

                CompletableFuture.allOf(skuCompletableFuture, saleAttrValueCompletableFuture, salesCompletableFuture, wareCompletableFuture).join();


                return orderItemVO;
            }).collect(Collectors.toList());

            confirmVO.setOrderItems(orderItemVOS);

        }, threadPoolExecutor);


        //4、基于雪花算法内置在mybatis的随机数
        CompletableFuture<Void> tokenCompletableFuture = CompletableFuture.runAsync(() -> {
            String idStr = IdWorker.getIdStr();
            confirmVO.setOrderToken(idStr);
            this.redisTemplate.opsForValue().set(AppConstant.TOKEN_PREFIX + idStr, idStr);//在redis中存一分
        }, threadPoolExecutor);



        CompletableFuture.allOf(umsCompletableFuture,memberCompletableFuture,bigSkuCompletableFuture,tokenCompletableFuture).join();

        return confirmVO;

    }
}
