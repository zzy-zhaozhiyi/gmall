package com.atguigu.gmall.index.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-13 18:28
 */
@Service
public class serviceImpl implements IndexService {
    @Autowired
    private GmallPmsClient gmallPmsClient;

    private static final String KEY_PREFIX = "index:cates:";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public List<CategoryEntity> querylevel1Categories() {
        Resp<List<CategoryEntity>> listResp = this.gmallPmsClient.queryCategory(1, null);//父id传0l也可以
        return listResp.getData();
    }

    @Override
    @GmallCache(prefix = "index:cates:")
    public List<CategoryVo> querySubCategery(Long pid) {


        Resp<List<CategoryVo>> listResp = gmallPmsClient.querySubCategory(pid);
        return listResp.getData();
}
    /*
    没有手写注解和AOP之前的方法，改造后的在上
     */
   /* @Override
    public List<CategoryVo> querySubCategery(Long pid) {
        // 1. 判断缓存中有没有
        String cateJson = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);

        // 2. 有，直接返回
        if (!StringUtils.isEmpty(cateJson)) {
            List<CategoryVo> categoryVos = JSON.parseArray(cateJson, CategoryVo.class);
            return categoryVos;
        }

        RLock lock = this.redissonClient.getLock("lock" + pid);//之所以加id是为了区别资源，只锁住自己的资源，而不影响其他资源的运行
        lock.lock();
        //  这里之所以判断两次时候高并发情况下的问题，和单例饿加代码块差不多的思想。

        //防止的是第一次判断的没有，1000个请求都进来，每个加锁后都要往缓存中加一次


        // 1. 判断缓存中有没有
        String cateJson2 = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
        // 2. 有，直接返回
        if (!StringUtils.isEmpty(cateJson2)) {
            lock.unlock();
            return JSON.parseArray(cateJson2, CategoryVo.class);
        }

        // 查询数据库
        Resp<List<CategoryVo>> listResp = gmallPmsClient.querySubCategory(pid);
        List<CategoryVo> categoryVOS = listResp.getData();
        // 3. 查询完成后放入缓存,加入了随机因子，防止雪崩
        this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryVOS), 7 + new Random().nextInt(5), TimeUnit.DAYS);

        lock.unlock();

        return listResp.getData();
    }*/
}
