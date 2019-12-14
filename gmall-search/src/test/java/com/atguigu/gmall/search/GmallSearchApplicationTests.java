package com.atguigu.gmall.search;

import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.search.vo.GoodsVO;
import com.atguigu.gmall.search.vo.SearchAttrVO;
import com.atguigu.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {
    @Autowired
    private ElasticsearchRestTemplate restTemplate;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GoodsRepository goodsRepository;

    @Test
//这是建索引库和映射
    void contextLoads() {
        this.restTemplate.createIndex(GoodsVO.class);
        this.restTemplate.putMapping(GoodsVO.class);
    }

    @Test
    void importData() {
        //进行分页查询
        Long pageNumber = 1L;
        Long pageSize = 100L;

        //分页查询spu



        do {//之所以要分页查询是为了不一次性查出天文数字条，导致奔溃
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setPage(pageNumber);
            queryCondition.setLimit(pageSize);
            Resp<List<SpuInfoEntity>> spuResp = this.pmsClient.queryPage(queryCondition);
            List<SpuInfoEntity> spus = spuResp.getData();

            //遍历spu,查询sku



          spus.forEach(spuInfoEntity -> {
                Resp<List<SkuInfoEntity>> skuResp = this.pmsClient.querySkusBySpuId(spuInfoEntity.getId());
                List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
                if (!CollectionUtils.isEmpty(skuInfoEntities)) {
                    //把sku转GoodVo

                    List<GoodsVO> goodlist = skuInfoEntities.stream().map(skuInfoEntity -> {
                        GoodsVO goodsVO = new GoodsVO();
                        //查询搜索属性以及值
                        Resp<List<ProductAttrValueEntity>> attrValueByResp = this.pmsClient.querySearchAttrValueBySpuId(spuInfoEntity.getId());
                        List<ProductAttrValueEntity> attrValueEntities = attrValueByResp.getData();
                        if (!CollectionUtils.isEmpty(attrValueEntities)) {//判断不为空在进行操作
                            List<SearchAttrVO> searchAttrVOS = attrValueEntities.stream().map(productAttrValueEntity -> {
                                SearchAttrVO searchAttrVO = new SearchAttrVO();
                                searchAttrVO.setAttrId(productAttrValueEntity.getAttrId());
                                searchAttrVO.setAttrName(productAttrValueEntity.getAttrName());
                                searchAttrVO.setAttrValue(productAttrValueEntity.getAttrValue());
                                return searchAttrVO;

                            }).collect(Collectors.toList());
                            goodsVO.setAttrs(searchAttrVOS);
                        }


                        goodsVO.setSkuId(skuInfoEntity.getSkuId());

                        goodsVO.setPic(skuInfoEntity.getSkuDefaultImg());

                        goodsVO.setTitle(skuInfoEntity.getSkuTitle());

                        goodsVO.setPrice(skuInfoEntity.getPrice().doubleValue());

                        goodsVO.setSale(0l);

                        goodsVO.setCreatTime(spuInfoEntity.getCreateTime());


                        //设置库存
                        Resp<List<WareSkuEntity>> listResp = this.wmsClient.queryWareSkuBySkuId(skuInfoEntity.getSkuId());
                        List<WareSkuEntity> WareSkuEntities = listResp.getData();
                        //这里的意思是只要在所有的仓库中显示有货就可以了，并不是显示具体的数量
                        boolean anyMatch = WareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                        goodsVO.setStore(anyMatch);

                        //设置品牌的名字
                        Resp<BrandEntity> brandEntityResp = this.pmsClient.queryBrandById(skuInfoEntity.getBrandId());

                        BrandEntity brandEntity = brandEntityResp.getData();
                        if (brandEntity != null) {
                            goodsVO.setBrandId(skuInfoEntity.getBrandId());
                            goodsVO.setBrandName(brandEntity.getName());
                        }

                        //设置分类的名字
                        Resp<CategoryEntity> categoryEntityResp = this.pmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
                        CategoryEntity categoryEntity = categoryEntityResp.getData();
                        if (categoryEntity != null) {
                            goodsVO.setCategoryId(skuInfoEntity.getCatalogId());
                            goodsVO.setCategoryName(categoryEntity.getName());
                        }


                        return goodsVO;
                    }).collect(Collectors.toList());
                    this.goodsRepository.saveAll(goodlist);
                }

            });


            //导入索引库


            pageSize = (long) spus.size();
            pageNumber++;

        } while (pageSize == 100);

    }
}
