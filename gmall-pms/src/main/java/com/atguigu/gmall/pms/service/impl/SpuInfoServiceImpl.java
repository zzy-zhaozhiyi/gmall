package com.atguigu.gmall.pms.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.pms.dao.SkuInfoDao;
import com.atguigu.gmall.pms.dao.SpuInfoDao;
import com.atguigu.gmall.pms.dao.SpuInfoDescDao;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.BaseAttrVo;
import com.atguigu.gmall.pms.vo.SkuInfoVo;
import com.atguigu.gmall.pms.vo.SpuInfoVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private ProductAttrValueService productAttrValueService;
    @Autowired
    private SpuInfoDescDao descDao;
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private SkuInfoDao skuInfoDao;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private AmqpTemplate amqpTemplate;
    //将交换机的名字注入进来
    @Value("${item.rabbitmq.exchange}")
    private String EXCHANGE_NAME;


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo   querySpuInfo(QueryCondition condition, Long catId) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        if (catId != 0) {
            //==0是全局查了，！=0就是按照本类来查
            wrapper.eq("catalog_id", catId);
        }
        //按照关键字来查
        String key = condition.getKey();
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(t -> t.eq("id", key)).or().like("spu_name", key);
            //上面的sql语句是这样的select *from spuinfo where catid=225 and(id=key or spu_name = key)
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(condition),
                wrapper
        );

        return new PageVo(page);
    }

    @Override
    @GlobalTransactional
    public void saveSpuInfoVo(SpuInfoVo spuInfoVo) {
        //1、保存spu相关的信息
        //1.1保存spuinfo的相关信息,spuidfovo里面没有的值，要先进行初始化的工作
        Long spuId = this.saveSpuinfo(spuInfoVo);

        //1.2保存spuinfo附属表的信息spu_info_desc的描述字段
        this.spuInfoDescService.saveSpuDesc(spuInfoVo, spuId);


        //1.3将基本信息baseattr进行保存
        this.saveBaseAttrs(spuInfoVo, spuId);

        //2、保存sku相关的信息,不能批量保存，因为他们都是独立的表,每次保存sku相关信息也会保存sms打折等这些信息
        this.saveSkuInfoWithSaleInfo(spuInfoVo, spuId);
        //就要用上这个方法,添加一个spu的时候，就将他发给es
        sendMsg("insert", spuId);
    }

    @ApiOperation("这是发送rabbitmq的消息方法")
    private void sendMsg(String type, Long spuId) {
        this.amqpTemplate.convertAndSend(EXCHANGE_NAME, "item." + type, spuId);
    }

    private void saveSkuInfoWithSaleInfo(SpuInfoVo spuInfoVo, Long spuId) {
        List<SkuInfoVo> skus = spuInfoVo.getSkus();
        if (!CollectionUtils.isEmpty(skus)) {
            skus.forEach(skuInfoVo -> {
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(skuInfoVo, skuInfoEntity);
                //获取品牌和分类的id从spuinfo中,设置skucode
                skuInfoEntity.setCatalogId(spuInfoVo.getCatalogId());
                skuInfoEntity.setBrandId(spuInfoVo.getBrandId());
                skuInfoEntity.setSkuCode(UUID.randomUUID().toString().replace("-", "").substring(0, 6));
                //获取默认图片
                List<String> images = skuInfoVo.getImages();
                if (!CollectionUtils.isEmpty(images)) {
                    skuInfoEntity.setSkuDefaultImg(skuInfoEntity.getSkuDefaultImg() == null ? images.get(0) : skuInfoEntity.getSkuDefaultImg());
                }
                skuInfoEntity.setSpuId(spuId);//上面保存spuinfo信息时，产生的自增主键
                this.skuInfoDao.insert(skuInfoEntity);
                //获得自增的skuid下面备用
                Long skuId = skuInfoEntity.getSkuId();
                //2.2保存sku_images的操作
                if (!CollectionUtils.isEmpty(images)) {//images是一个string 的集合要转成对象集合
                    List<SkuImagesEntity> imagesEntities = images.stream().map(image -> {
                        SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                        skuImagesEntity.setImgUrl(image);
                        skuImagesEntity.setSkuId(skuId);
                        skuImagesEntity.setDefaultImg(StringUtils.equals(skuInfoVo.getSkuDefaultImg(), image) ? 1 : 0);
                        return skuImagesEntity;
                    }).collect(Collectors.toList());
                    this.skuImagesService.saveBatch(imagesEntities);
                }
                //2.3保存 pms_sale_attr_value
                List<SkuSaleAttrValueEntity> saleAttrs = skuInfoVo.getSaleAttrs();
                if (!CollectionUtils.isEmpty(saleAttrs)) {
                    saleAttrs.forEach(skuSaleAttrValueEntity -> skuSaleAttrValueEntity.setSkuId(skuId));
                    this.skuSaleAttrValueService.saveBatch(saleAttrs);
                }

                //3、保存营销（他和sku是绑定的一起操作）相关的信息，当然这涉及到远程调用，尽量少的调用
                //三张表的信息，要依次进行保存，sms_sku_bounds,sms_sku_ladder,sms_sku_full_reduction

                SkuSaleVo skuSaleVo = new SkuSaleVo();
                BeanUtils.copyProperties(skuInfoVo, skuSaleVo);
                skuSaleVo.setSkuId(skuId);

                gmallSmsClient.saveSkuSaleVo(skuSaleVo);


            });
        }
    }

    private void saveBaseAttrs(SpuInfoVo spuInfoVo, Long spuId) {
        List<BaseAttrVo> baseAttrs = spuInfoVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {//这种直接交互的都要进行判断，避免增加io压力
            List<ProductAttrValueEntity> productAttrValueEntities = baseAttrs.stream().
                    map(baseAttrVo -> {
                        ProductAttrValueEntity AttrValueEntity = baseAttrVo;
                        AttrValueEntity.setSpuId(spuId);
                        return AttrValueEntity;
                    }).collect(Collectors.toList());
            this.productAttrValueService.saveBatch(productAttrValueEntities);
        }
    }
//这部分移到了spudesc中了，为了测试分布式事务
   /* private void saveSpuDesc(SpuInfoVo spuInfoVo, Long spuId) {
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        //这个附属表的id不是自增的，采用的是spuid的，但是我们设置了全局的自增，会没用，所以要在实体类进行修改成手动设置	@TableId(type = IdType.INPUT)
        spuInfoDescEntity.setSpuId(spuId);
        List<String> spuImages = spuInfoVo.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)) {//判断不为空才进行操作，避免增加io的压力
            spuInfoDescEntity.setDecript(StringUtils.join(spuImages, ","));//转成string类型的字符串保存
            //this.descDao.insert(spuInfoDescEntity);//这里的service方法即可完成工作，都可以完成工作
            this.spuInfoDescService.save(spuInfoDescEntity);
        }
    }*/

    private Long saveSpuinfo(SpuInfoVo spuInfoVo) {
        spuInfoVo.setPublishStatus(0);
        spuInfoVo.setCreateTime(new Date());
        spuInfoVo.setUodateTime(spuInfoVo.getCreateTime());//这里的跟新时间和创造时间是一样的，但是不能new会有误差的，上面已设置
        this.save(spuInfoVo);
        return spuInfoVo.getId();
    }

}