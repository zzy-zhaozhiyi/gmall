package com.atguigu.gmall.order.service;

import com.atguigu.core.AppConstant;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.MemberException;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.cart.vo.CartVO;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptors.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import com.atguigu.gmall.oms.vo.OrderItemVO;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private GmallOmsClient omsClient;
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
    @Autowired
    private AmqpTemplate amqpTemplate;

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


        //4、基于雪花算法内置在mybatis的随机数1
        CompletableFuture<Void> tokenCompletableFuture = CompletableFuture.runAsync(() -> {
            String idStr = IdWorker.getIdStr();
            confirmVO.setOrderToken(idStr);
            this.redisTemplate.opsForValue().set(AppConstant.TOKEN_PREFIX + idStr, idStr);//在redis中存一分
        }, threadPoolExecutor);


        CompletableFuture.allOf(umsCompletableFuture, memberCompletableFuture, bigSkuCompletableFuture, tokenCompletableFuture).join();
        return confirmVO;


    }

    public OrderEntity submit(OrderSubmitVO submitVO) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        // 1. 防重复提交，查询redis中有没有orderToken信息，有，则是第一次提交，放行并删除redis中的orderToken
        String orderToken = submitVO.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(AppConstant.TOKEN_PREFIX + orderToken), orderToken);
        if (flag == 0) {
            throw new OrderException("订单不可重复提交");
        }

        // 2. 校验价格，总价一致放行
        BigDecimal totalPrice = submitVO.getTotalPrice();
        List<OrderItemVO> orderItemVOS = submitVO.getItems();
        // 关于价格的问题，每次都要查询最新的，要跟数据库实时同步
        if (CollectionUtils.isEmpty(orderItemVOS)) {
            throw new OrderException("请勾选商品在进行提交");
        }
        BigDecimal currentPrice = orderItemVOS.stream().map(orderItemVO -> {
            SkuInfoEntity skuInfoEntity = this.pmsClient.querySkuInfoBySkuId(orderItemVO.getSkuId()).getData();
            if (skuInfoEntity != null) {//凡是下面通过这个来到值，都要进行判断
                BigDecimal price = skuInfoEntity.getPrice();
                BigDecimal bigDecimal = price.multiply(new BigDecimal(orderItemVO.getCount()));
                return bigDecimal;
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();
        if (totalPrice.compareTo(currentPrice) != 0) {
            throw new OrderException("价格有误，请重新刷新页面");

        }
        // 3. 校验库存是否充足并锁定库存，一次性提示所有库存不够的商品信息 （远程接口待开发）
        List<SkuLockVO> skuLockVOS = orderItemVOS.stream().map(orderItemVO -> {
            SkuLockVO skuLockVO = new SkuLockVO();
            skuLockVO.setCount(orderItemVO.getCount());
            skuLockVO.setOrderToken(orderToken);
            skuLockVO.setSkuId(orderItemVO.getSkuId());
            return skuLockVO;
        }).collect(Collectors.toList());
        Resp<Object> resp = this.wmsClient.checkAndLockStore(skuLockVOS);
        if (resp.getCode() != 0) {//0代表的是成功的状态码，1是失败的
            throw new OrderException(resp.getMsg());
        }


        // 4. 下单（创建订单及订单详情， 远程接口待开发）
        OrderEntity orderEntity = null;
        try {
            submitVO.setUserId(userInfo.getId());
            orderEntity = this.omsClient.saveOrder(submitVO).getData();
        } catch (Exception e) {
            e.printStackTrace();
            // 发送消息给wms，解锁对应的库存，保存出现异常，要及时回滚数据，用seata的性能太低了
            this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "stock.unlock", orderToken);
            throw new OrderException("创建订单失败，请联系服务人员");
        }


        // 5. 删除购物车 （发送消息删除购物车）,要根据userId和skuID来进行删除，因为我们用的是redis中的hash来存的<string,object,object>
        //从拦截器中，获得userId
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userInfo.getId());
        //删除提交的订单中所有的skuIds
        List<Long> skuIds = orderItemVOS.stream().map(OrderItemVO::getSkuId).collect(Collectors.toList());
        map.put("skuIds", skuIds);
        this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "cart.delete", map);
        if (orderEntity == null) {
            return null;
        }
        return orderEntity;
    }
}
/*
这里总结下订单调教在3、4、5步都是用到了mq
3：解决的是，order中调用wms的checkandlock方法时，调用成功，响应失败的情况，这时要用到定时解锁库存，这个操作在checkandlock中进行这样
避免远程调用失败的可能性，并有wmslistner监听私信队列，
4：是远程时创建订单失败，也要进行解锁库存，不用延时和死信队列
4：订单的定时关单，用户长时间的不付款，就要关单
5：删除购物车
 */