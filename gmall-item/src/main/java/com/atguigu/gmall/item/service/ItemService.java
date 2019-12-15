package com.atguigu.gmall.item.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVO;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author zzy
 * @create 2019-12-14 22:41
 */
@Service
public class ItemService {//其实应该写service接口的，这里省略了

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;

    public ItemVO queryItemVoBySkuId(Long skuid) {
        ItemVO itemVO = new ItemVO();

        itemVO.setSkuId(skuid);
        //1、根据skuid设置相关属性
        Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuInfoBySkuId(skuid);
        SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
        if (skuInfoEntity == null) {
            return itemVO;//，没有skuinfoentity说明没有这个商品，直接返回就可以了
        }
        itemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
        itemVO.setSubTitle(skuInfoEntity.getSkuSubtitle());
        itemVO.setPrice(skuInfoEntity.getPrice());
        itemVO.setWeight(skuInfoEntity.getWeight());

        //2、设置spu的相关信息，根据skuinfo获得spuid数据库设计的
        Long spuId = skuInfoEntity.getSpuId();
        itemVO.setSpuId(spuId);
        //根据spuid查询spu信息
        Resp<SpuInfoEntity> spuInfoEntityResp = this.pmsClient.querySpuInfoBySpuId(spuId);
        SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
        if (spuInfoEntity != null) {
            itemVO.setSpuName(spuInfoEntity.getSpuName());
        }
        //3、设置category和brand的entity根据skuid
        Resp<CategoryEntity> categoryEntityResp = this.pmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
        CategoryEntity categoryEntity = categoryEntityResp.getData();
        if (categoryEntity != null) {//其实categoryEntity没点东西，可以不判断
            itemVO.setCategoryEntity(categoryEntity);
        }
        Resp<BrandEntity> brandEntityResp = this.pmsClient.queryBrandById(skuInfoEntity.getBrandId());
        BrandEntity brandEntity = brandEntityResp.getData();
        if (brandEntity != null) {//其实brandentity没点东西，可以不判断
            itemVO.setBrandEntity(brandEntity);
        }
        //4、设置skuimageentity 根据skuid
        Resp<List<SkuImagesEntity>> listResp = this.pmsClient.querySkuImagesBySkuId(skuid);
        List<SkuImagesEntity> skuImagesEntities = listResp.getData();
        if (skuImagesEntities != null) {//其实skuImagesEntities没点东西，可以不判断
            itemVO.setPics(skuImagesEntities);
        }
        //5、查询营销的相关信息
        Resp<List<SaleVO>> voBySkuId = this.smsClient.querySalesVoBySkuId(skuid);
        List<SaleVO> voBySkuIdData = voBySkuId.getData();
        if (!CollectionUtils.isEmpty(voBySkuIdData)) {
            itemVO.setSales(voBySkuIdData);
        }
        //6、查询库存的信息
        Resp<List<WareSkuEntity>> wareSkuBySkuId = this.wmsClient.queryWareSkuBySkuId(skuid);
        List<WareSkuEntity> wareSkuEntities = wareSkuBySkuId.getData();
        boolean anyMatch = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
        itemVO.setStore(anyMatch);//只要查到任意一个仓库有库存，就是true,显示有货


        //7、根据spuid先查所有的sku，再 从查询销售属性的信息
        Resp<List<SkuSaleAttrValueEntity>> listResp1 = this.pmsClient.querySkuSaleAttrValueBySpuId(spuId);
        List<SkuSaleAttrValueEntity> data = listResp1.getData();
        itemVO.setSaleAttrs(data);

        //8、查询spu下面的海报
        Resp<SpuInfoDescEntity> spuInfoDescEntityResp = this.pmsClient.querySpuInfoDescBySpuId(spuId);
        SpuInfoDescEntity spuInfoDescEntity = spuInfoDescEntityResp.getData();
        if (spuInfoDescEntity != null) {
            String decript = spuInfoDescEntity.getDecript();
            //因为海报可能是多张的，并且用逗号进行隔开，所以进行分割
            String[] split = StringUtils.split(decript, ",");
            //vo里面是list，所以转成数组转成list
            itemVO.setImages(Arrays.asList(split));
        }

        //9、设置规格参数以及下面的参数值
        Resp<List<ItemGroupVO>> listResp2 = this.pmsClient.queryItemGroupVoBySpuIdAndcatId(spuId, skuInfoEntity.getCatalogId());
        List<ItemGroupVO> itemGroupVOS = listResp2.getData();
        itemVO.setGroups(itemGroupVOS);


        return itemVO;
    }
}
