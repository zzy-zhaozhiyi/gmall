package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zzy
 * @create 2019-12-09 18:57
 */

public interface GmallPmsApi {
    @GetMapping("pms/category")
    public Resp<List<CategoryEntity>> queryCategory(@RequestParam(value="level", defaultValue = "0")Integer level
            , @RequestParam(value="parentCid", required = false)Long parentCid);


    @GetMapping("pms/spuinfo/info/{id}")
    public Resp<SpuInfoEntity> querySpuInfoBySpuId(@PathVariable("id") Long id);

    @PostMapping("pms/spuinfo/page")
    public Resp<List<SpuInfoEntity>> queryPage(@RequestBody QueryCondition queryCondition);

    @PostMapping("/pms/spuinfo/list")
    Resp<PageVo> list(@RequestBody QueryCondition queryCondition);


    @GetMapping("pms/brand/info/{brandId}")
    public Resp<BrandEntity> queryBrandById(@PathVariable("brandId") Long brandId);


    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCategoryById(@PathVariable("catId") Long catId);

    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkusBySpuId(@PathVariable("spuId") Long spuId);


    @GetMapping("pms/productattrvalue/{spuId}")
    public Resp<List<ProductAttrValueEntity>> querySearchAttrValueBySpuId(@PathVariable("spuId") Long spuId);


    @GetMapping("pms/category/{pid}")
     Resp<List<CategoryVo>> querySubCategory(@PathVariable("pid") Long pid);

    @GetMapping("pms/skuinfo/info/{skuId}")
    public Resp<SkuInfoEntity> querySkuInfoBySkuId(@PathVariable("skuId") Long skuId);

    @GetMapping("pms/skuimages/{skuid}")
    public Resp<List<SkuImagesEntity>> querySkuImagesBySkuId(@PathVariable("skuid")Long skuid);



    @ApiOperation("根据spuid来查询其下面的所有sku信息")
    @GetMapping("pms/skusaleattrvalue/{spuid}")
    public  Resp<List<SkuSaleAttrValueEntity>> querySkuSaleAttrValueBySpuId(@PathVariable("spuid")Long spuid);


    @ApiOperation("详情查询")
    @GetMapping("pms/spuinfodesc/info/{spuId}")
    public Resp<SpuInfoDescEntity> querySpuInfoDescBySpuId(@PathVariable("spuId") Long spuId);



    @ApiOperation("查询规格参数以及下面的参数值")
    @GetMapping("pms/attrgroup/item/group/{spuid}/{catid}")
    public Resp<List<ItemGroupVO>> queryItemGroupVoBySpuIdAndcatId(@PathVariable("spuid")Long spuid, @PathVariable("catid")Long catid);







}
