package com.atguigu.gmall.sms.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.sms.dao.SkuBoundsDao;
import com.atguigu.gmall.sms.dao.SkuFullReductionDao;
import com.atguigu.gmall.sms.dao.SkuLadderDao;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsDao, SkuBoundsEntity> implements SkuBoundsService {
    @Autowired
    private SkuLadderDao skuLadderDao;
    @Autowired
    private SkuFullReductionDao reductionDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuBoundsEntity> page = this.page(
                new Query<SkuBoundsEntity>().getPage(params),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageVo(page);
    }

    @Override
    @Transactional
    public void saveSkuSaleVo(SkuSaleVo skuSaleVo) {
        // 3.1. 保存sms_sku_bounds
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        skuBoundsEntity.setSkuId(skuSaleVo.getSkuId());
        skuBoundsEntity.setGrowBounds(skuSaleVo.getGrowBounds());
        skuBoundsEntity.setBuyBounds(skuSaleVo.getBuyBounds());
        List<Integer> work = skuSaleVo.getWork();
        skuBoundsEntity.setWork(work.get(3) * 1 + work.get(2) * 2 + work.get(1) * 4 + work.get(0) * 8);
        this.save(skuBoundsEntity);

        // 3.2. 保存sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuSaleVo.getSkuId());
        skuLadderEntity.setFullCount(skuSaleVo.getFullCount());
        skuLadderEntity.setDiscount(skuSaleVo.getDiscount());
        skuLadderEntity.setAddOther(skuSaleVo.getLadderAddOther());
        this.skuLadderDao.insert(skuLadderEntity);

        // 3.3. 保存sms_sku_full_reduction
        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        reductionEntity.setSkuId(skuSaleVo.getSkuId());
        reductionEntity.setFullPrice(skuSaleVo.getFullPrice());
        reductionEntity.setReducePrice(skuSaleVo.getReducePrice());
        reductionEntity.setAddOther(skuSaleVo.getFullAddOther());
        this.reductionDao.insert(reductionEntity);
    }

    @Override
    public List<SaleVO> querySalesVoBySkuId(Long skuid) {
        //这是在skubounds中进行的三个营销信息的查询,先创造一个list<saleVo>,将三个营销信息放入
        List<SaleVO> saleVOS = new ArrayList<>();

        //1、查询skubounds 买赠积分的信息
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuid));
        //1.1创造一个salevo放买赠积分的
        SaleVO boundVo = new SaleVO();
        boundVo.setType("买赠积分");
        if (skuBoundsEntity != null) {//这里涉及到了skuboundsentity点，所以要判断下
            //1.2，买赠积分两个，任意一个可能为空，如果两个都有的话中间还涉及到，怎样分割的问题,这里就创造一个stringbuffer来进行拼接
            StringBuffer sb = new StringBuffer();
            if (skuBoundsEntity.getGrowBounds() != null & skuBoundsEntity.getGrowBounds().intValue() > 0) {
                sb.append("成长积分送" + skuBoundsEntity.getGrowBounds());
            }//两个积分是独立的
            if ((skuBoundsEntity.getBuyBounds() != null & skuBoundsEntity.getBuyBounds().intValue() > 0)) {
                if (sb != null) {
                    sb.append(",");//判断前面拼接不为空，就用逗号分割
                }
                sb.append("赠送积分是" + skuBoundsEntity.getBuyBounds());
            }
            boundVo.setDesc(sb.toString());
            saleVOS.add(boundVo);
        }

       //买减的操作,下面连个就没有买赠积分的这么多判断，因为他们是一体的
        SkuFullReductionEntity reductionEntity = this.reductionDao.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuid));
        if(reductionEntity!=null){
            SaleVO fullReduceVo = new SaleVO();
            fullReduceVo.setType("买减信息");
            fullReduceVo.setDesc("买满"+reductionEntity.getFullPrice()+",减"+reductionEntity.getReducePrice());
            saleVOS.add(fullReduceVo);
        }

        //买打折的操作
        SkuLadderEntity ladderEntity = this.skuLadderDao.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuid));
        if(ladderEntity!=null){
            SaleVO ladderVo = new SaleVO();
            ladderVo.setType("打折");
            ladderVo.setDesc("买"+ladderEntity.getFullCount()+"件，打"+ladderEntity.getDiscount()+"折");
            saleVOS.add(ladderVo);
        }

        return saleVOS;
    }

}