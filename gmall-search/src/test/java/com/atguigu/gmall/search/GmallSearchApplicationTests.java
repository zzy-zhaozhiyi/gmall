package com.atguigu.gmall.search;

import com.atguigu.gmall.search.vo.GoodsVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

@SpringBootTest
class GmallSearchApplicationTests {
@Autowired
private ElasticsearchRestTemplate restTemplate;
    @Test
    void contextLoads() {
        this.restTemplate.createIndex(GoodsVO.class);
        this.restTemplate.putMapping(GoodsVO.class);
    }

}
