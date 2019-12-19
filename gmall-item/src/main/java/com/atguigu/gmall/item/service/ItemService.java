package com.atguigu.gmall.item.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVO;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

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
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    //异步编排的话，根据方法传参的值进行的运行，可以新开一个异步编排，需要依靠别人的返回值的九合一串行化；
    public ItemVO queryItemVoBySkuId(Long skuid) {
        ItemVO itemVO = new ItemVO();

        itemVO.setSkuId(skuid);
        //1、根据skuid设置相关属性
        CompletableFuture<Object> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuInfoBySkuId(skuid);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                return itemVO;//，没有skuinfoentity说明没有这个商品，直接返回就可以了
            }
            itemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
            itemVO.setSubTitle(skuInfoEntity.getSkuSubtitle());
            itemVO.setPrice(skuInfoEntity.getPrice());
            itemVO.setWeight(skuInfoEntity.getWeight());
            itemVO.setSpuId(skuInfoEntity.getSpuId());
            return skuInfoEntity;
        }, threadPoolExecutor);


        //2、根据spuid查询spu信息
        CompletableFuture<Void> spuCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoEntity> spuInfoEntityResp = this.pmsClient.querySpuInfoBySpuId(((SkuInfoEntity) skuInfoEntity).getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
            if (spuInfoEntity != null) {
                itemVO.setSpuName(spuInfoEntity.getSpuName());
            }
        }, threadPoolExecutor);
        //3、设置category根据skuid
        CompletableFuture<Void> categoryCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<CategoryEntity> categoryEntityResp = this.pmsClient.queryCategoryById(((SkuInfoEntity) skuInfoEntity).getCatalogId());
            CategoryEntity categoryEntity = categoryEntityResp.getData();
            if (categoryEntity != null) {//其实categoryEntity没点东西，可以不判断
                itemVO.setCategoryEntity(categoryEntity);
            }
        }, threadPoolExecutor);

        ///4、设置brand的entity根据skuid
        CompletableFuture<Void> brandCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<BrandEntity> brandEntityResp = this.pmsClient.queryBrandById(((SkuInfoEntity) skuInfoEntity).getBrandId());
            BrandEntity brandEntity = brandEntityResp.getData();
            if (brandEntity != null) {//其实brandentity没点东西，可以不判断
                itemVO.setBrandEntity(brandEntity);
            }
        }, threadPoolExecutor);

        //5、设置skuimagestity 根据skuid
        CompletableFuture<Void> skuImageCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<SkuImagesEntity>> listResp = this.pmsClient.querySkuImagesBySkuId(skuid);
            List<SkuImagesEntity> skuImagesEntities = listResp.getData();
            if (skuImagesEntities != null) {//其实skuImagesEntities没点东西，可以不判断
                itemVO.setPics(skuImagesEntities);
            }
        }, threadPoolExecutor);


        //6、查询营销的相关信息
        CompletableFuture<Void> SaleCompletableFuture = CompletableFuture.runAsync(() -> {

            Resp<List<SaleVO>> voBySkuId = this.smsClient.querySalesVoBySkuId(skuid);
            List<SaleVO> voBySkuIdData = voBySkuId.getData();
            if (!CollectionUtils.isEmpty(voBySkuIdData)) {
                itemVO.setSales(voBySkuIdData);
            }
        }, threadPoolExecutor);
        //7、查询库存的信息
        CompletableFuture<Void> wareSkuCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<WareSkuEntity>> wareSkuBySkuId = this.wmsClient.queryWareSkuBySkuId(skuid);
            List<WareSkuEntity> wareSkuEntities = wareSkuBySkuId.getData();
            boolean anyMatch = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
            itemVO.setStore(anyMatch);//只要查到任意一个仓库有库存，就是true,显示有货
        }, threadPoolExecutor);


        //8、根据spuid先查所有的sku，再 从查询销售属性的信息
        CompletableFuture<Void> SkuSaleAttrValueCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfoEntity -> {

            Resp<List<SkuSaleAttrValueEntity>> listResp1 = this.pmsClient.querySkuSaleAttrValueBySpuId(((SkuInfoEntity) skuInfoEntity).getSpuId());
            List<SkuSaleAttrValueEntity> data = listResp1.getData();
            itemVO.setSaleAttrs(data);
        }, threadPoolExecutor);

        //9、查询spu下面的海报
        CompletableFuture<Void> SpuInfoDescCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfoEntity -> {

            Resp<SpuInfoDescEntity> spuInfoDescEntityResp = this.pmsClient.querySpuInfoDescBySpuId(((SkuInfoEntity) skuInfoEntity).getSpuId());
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescEntityResp.getData();
            if (spuInfoDescEntity != null) {
                String decript = spuInfoDescEntity.getDecript();
                //因为海报可能是多张的，并且用逗号进行隔开，所以进行分割
                String[] split = StringUtils.split(decript, ",");
                //vo里面是list，所以转成数组转成list
                itemVO.setImages(Arrays.asList(split));
            }
        }, threadPoolExecutor);

        //10、设置规格参数以及下面的参数值
        CompletableFuture<Void> itemGroupCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<ItemGroupVO>> listResp2 = this.pmsClient.queryItemGroupVoBySpuIdAndcatId(((SkuInfoEntity) skuInfoEntity).getSpuId(), ((SkuInfoEntity) skuInfoEntity).getCatalogId());
            List<ItemGroupVO> itemGroupVOS = listResp2.getData();
            itemVO.setGroups(itemGroupVOS);
        }, threadPoolExecutor);

        //之所以添加这个是因为，等这些 异步编排结束后在进行保存；么有skuInfoCompletableFuture的原因是，依赖他的执行完了，他肯定执行完了
        CompletableFuture.allOf(spuCompletableFuture, categoryCompletableFuture, brandCompletableFuture,
                skuImageCompletableFuture, SaleCompletableFuture,
                wareSkuCompletableFuture, SkuSaleAttrValueCompletableFuture,
                SpuInfoDescCompletableFuture, itemGroupCompletableFuture).join();//这里的用join和get都可以，但是这里自己弄得时候忘加了

        return itemVO;
    }
}
