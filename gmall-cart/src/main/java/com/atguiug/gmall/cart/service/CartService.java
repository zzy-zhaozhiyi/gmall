package com.atguiug.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.AppConstant;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.cart.vo.CartVO;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.wms.entity.WareSkuEntity;
import com.atguiug.gmall.cart.feign.GmallPmsClient;
import com.atguiug.gmall.cart.feign.GmallSmsClient;
import com.atguiug.gmall.cart.feign.GmallWmsClient;
import com.atguiug.gmall.cart.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zzy
 * @create 2019-12-17 18:08
 */
@Service
public class CartService {
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;



    public void addCart(CartVO cartvo) {

        //先判断是否登录,通过ThreadLocal提供的方法，获得拦截信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String key = AppConstant.KEY_PREFIX;
        if (userInfo.getId() != null) {
            key += userInfo.getId();
        } else {
            key += userInfo.getUserKey();
        }                                 //redis中的hasn<string,<string,object>>
        //我们购物车采用的是redis中hash结构<user,map<skuid,cart>>类似这样的结构，所以采用下面的这种方式操作hash更好点
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        //判断是否存在购物车，根据map中是否含有skuid
        String skuId = cartvo.getSkuId().toString();//redis中的都是字符串，所以也必须转成字符串，放进去查询，不然没用

        if (hashOps.hasKey(skuId)) {
            //说明购物车已经存在只要跟新数量即可
            String cartJson = hashOps.get(skuId).toString();
            CartVO cart1 = JSON.parseObject(cartJson, CartVO.class);
            cart1.setCount(cart1.getCount() + cartvo.getCount());
            hashOps.put(skuId, JSON.toJSONString(cart1));
        } else {
            //没有这个购物车，就要添加新的了,虽然我们封装了cartVo,但是里面只有skuid和count其他都要查询设置
            cartvo.setCheck(true);

            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuInfoBySkuId(cartvo.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                return;
            }
            cartvo.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
            cartvo.setTitle(skuInfoEntity.getSkuTitle());
            cartvo.setPrice(skuInfoEntity.getPrice());


            //价格存两份，实时的价格在redis中的结构是{skuid,currentPrice}
            this.redisTemplate.opsForValue().set(AppConstant.PRICE_PREFIX + skuInfoEntity.getSkuId(), skuInfoEntity.getPrice().toString());



            Resp<List<SkuSaleAttrValueEntity>> value = this.pmsClient.querySkuSaleAttrValueBySkuId(cartvo.getSkuId());
            cartvo.setSaleAttrValues(value.getData());

            List<SaleVO> saleVOS = this.smsClient.querySalesVoBySkuId(cartvo.getSkuId()).getData();
            cartvo.setSales(saleVOS);


            List<WareSkuEntity> wareSkuEntities = this.wmsClient.queryWareSkuBySkuId(cartvo.getSkuId()).getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                //只要判断这些仓库中有一个有货就可以了
                boolean b = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                cartvo.setStore(b);
            }
            hashOps.put(skuId, JSON.toJSONString(cartvo));
        }


    }

    public List<CartVO> queryCart() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        //先查未登录状态的购物车，再查登录状态的，这样简便些，利于未登录合并道已登录
        String unLoginKey = AppConstant.KEY_PREFIX + userInfo.getUserKey();
        BoundHashOperations<String, Object, Object> unHasnOps = this.redisTemplate.boundHashOps(unLoginKey);//得到里面的map
        //利用map的一个value方法
        List<Object> values = unHasnOps.values();//将其转化成实体类
        List<CartVO> unLoginVOS = null;//下面要用所以拿出来
        if (!CollectionUtils.isEmpty(values)) {
            unLoginVOS = values.stream().map(value -> {

                        CartVO cartVO = JSON.parseObject(value.toString(), CartVO.class);//解析第一个是参数是string，value是objet，所以需要强转

                        String price = this.redisTemplate.opsForValue().get(AppConstant.PRICE_PREFIX + cartVO.getSkuId());
                        cartVO.setCurrentPrice(new BigDecimal(price));


                        return cartVO;
                    }
            ).collect(Collectors.toList());
        }

        //判断是否登录，看userinfo里的id是否存在就行
        if (userInfo.getId() == null) {
            return unLoginVOS;//就返回未登录的
        }
        //登录，购物车同步
        String LoginKey = AppConstant.KEY_PREFIX + userInfo.getId();
        BoundHashOperations<String, Object, Object> loginOps = this.redisTemplate.boundHashOps(LoginKey);
        if (!CollectionUtils.isEmpty(unLoginVOS)) {

            unLoginVOS.forEach(unLoginVO -> {
                String skuId = unLoginVO.getSkuId().toString();
                //如果登录状态是和未登录时的sku产品相同，就跟新数量
                if (loginOps.hasKey(skuId)) {
                    String loginJson = loginOps.get(skuId).toString();//之所以转成String是为了解析的需要,第一个参数是String
                    CartVO cartVO = JSON.parseObject(loginJson, CartVO.class);
                    cartVO.setCount(cartVO.getCount() + unLoginVO.getCount());
                    //添加实时的价格同步
                    String price = this.redisTemplate.opsForValue().get(AppConstant.PRICE_PREFIX + skuId);
                    cartVO.setCurrentPrice(new BigDecimal(price));


                    loginOps.put(skuId, JSON.toJSONString(cartVO));

                } else {
                        //添加实时的价格同步
                    String price = this.redisTemplate.opsForValue().get(AppConstant.PRICE_PREFIX + skuId);
                    unLoginVO.setCurrentPrice(new BigDecimal(price));

                    //如果在登录状态中没有的，就添加到里面，必须要放在else中不然会执行。
                    loginOps.put(skuId, JSON.toJSONString(unLoginVO));
                }

            });
            this.redisTemplate.delete(unLoginKey);

        }
        List<Object> loginCarVoJsons = loginOps.values();
        //依次遍历进行转成实体
        List<CartVO> cartVOS = loginCarVoJsons.stream().map(loginCartVo -> JSON.parseObject(loginCartVo.toString(), CartVO.class)).collect(Collectors.toList());

        return cartVOS;
    }

    public void updateCart(CartVO cartVO) {
        //判断登录状态，修改对应状态的cart
        String key = AppConstant.KEY_PREFIX;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getId() != null) {
            key += userInfo.getId();
        } else {
            key += userInfo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        String skuId = cartVO.getSkuId().toString();
        if (hashOps.hasKey(skuId)) {
            String s = hashOps.get(skuId).toString();
            //解析第一个参数字符串，
            CartVO cartVO1 = JSON.parseObject(s, CartVO.class);
            cartVO1.setCount(cartVO.getCount());

            String price = this.redisTemplate.opsForValue().get(AppConstant.PRICE_PREFIX + skuId);
            cartVO1.setCurrentPrice(new BigDecimal(price));

            hashOps.put(skuId, JSON.toJSONString(cartVO1));
        }


    }

    public void deleteCart(Long skuid) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String key = AppConstant.KEY_PREFIX;
        if (userInfo.getId() != null) {
            key += userInfo.getId();
        } else {
            key += userInfo.getUserKey();
        }
        //只是删除skuid对应的cart，不是删除外面的那个拥有者
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        hashOps.delete(skuid.toString());
        //之所以有tostring,记住redis的结构是<string,<string,object>>


    }

    public List<CartVO> queryCheckAndCartByUserId(Long userId) {
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(AppConstant.KEY_PREFIX + userId);
        List<Object> values = hashOps.values();//将其进行解析，还原成购物项
        //先转化在过滤
        List<CartVO> cartVOS = values.stream().map(value -> JSON.parseObject(value.toString(), CartVO.class)).filter(CartVO::getCheck).collect(Collectors.toList());
        return cartVOS;
    }
}
