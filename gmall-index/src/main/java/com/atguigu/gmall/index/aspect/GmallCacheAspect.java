package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author zzy
 * @create 2019-12-14 10:15
 */
@Aspect
@Component
public class GmallCacheAspect {

    /**
     * 1.返回值object
     * 2.参数proceedingJoinPoint
     * 3.抛出异常Throwable
     * 4.proceedingJoinPoint.proceed(args)执行业务方法
     */

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    @Around("@annotation(com.atguigu.gmall.index.annotation.GmallCache)")//拦截所有加了该注解的方法
    public Object cacheAroundAdvice(ProceedingJoinPoint point) throws Throwable {

        Object result = null;

        //获取连接点的签名，并且强转到方法的前面
        MethodSignature signature = (MethodSignature) point.getSignature();
        //获取连接点方法，并且获得方法上的注解，这样才能到的其中的注解中的前缀
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);
        //根据注解获得前缀和参数进行拼接
        String prefix = gmallCache.prefix();
        //获取方法的参数

        //拼接唯一性的
        String key = prefix + Arrays.asList(point.getArgs()).toString();

        //进行第一次的查询内存
        result = this.cacheHit(key, signature);
        if (result != null) {
            return result;
        }

        RLock rLock = this.redissonClient.getLock("lock" + Arrays.asList(point.getArgs()));//错误的原因是锁的名字用的是key,到时Redisson混乱

        rLock.lock();

        //进行第二次的查询缓存
        result = this.cacheHit(key, signature);
        if (result != null) {
            rLock.unlock();
            return result;
        }
        //进行了第二次的查询缓存，进行确认的话，还没有的话，就放一个请求去查询数据库放入缓存，其他的查缓存
        result = point.proceed(point.getArgs());
        this.stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(result));
        rLock.unlock();
        return result;

    }

    private Object cacheHit(String key, MethodSignature signature) {
        //查询缓存中有这个缓存
        String cacheJosn = this.stringRedisTemplate.opsForValue().get(key);
        //进行判断是否为空
        if (StringUtils.isNotBlank(cacheJosn)) {
            //解析字符串返回
            Object parseObject = JSON.parseObject(cacheJosn, signature.getReturnType());
            return parseObject;
        }
        return null;
    }


}
