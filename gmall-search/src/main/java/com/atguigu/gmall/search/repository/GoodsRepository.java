package com.atguigu.gmall.search.repository;

import com.atguigu.gmall.search.vo.GoodsVO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author zzy
 * @create 2019-12-10 18:15
 */

public interface GoodsRepository extends ElasticsearchRepository<GoodsVO,Long> {
    //不用写注解，因为它本身是springboot的就可以自动注入
}
