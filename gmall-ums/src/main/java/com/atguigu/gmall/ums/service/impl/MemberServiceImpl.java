package com.atguigu.gmall.ums.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.exception.MemberException;
import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
@Autowired
private StringRedisTemplate stringRedisTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public Boolean checkDataByType(String data, Integer type) {
        //通过构建querywapper来通过count统计个数来进行验证，也可以getone,list等
        QueryWrapper<MemberEntity> queryWrapper = new QueryWrapper<>();
        switch (type) {
            case 1:
                queryWrapper.eq("username", data);
                break;
            case 2:
                queryWrapper.eq("mobile", data);
                break;
            case 3:
                queryWrapper.eq("email", data);
                break;
            default:
                return false;

        }
        int count = this.count(queryWrapper);
        return count == 0;
    }

    @Override
    public void register(MemberEntity memberEntity, String code) {
        //1、判断手机验证码（没写，以后写了在做1和4）
        //2、生成salt,并进行加密操作,加salt和加盐密码存入DB
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        memberEntity.setSalt(salt);
        String slatpassword = DigestUtils.md5Hex(memberEntity.getPassword() + salt);
        memberEntity.setPassword(slatpassword);
        //3、设置新增用户的一些默认值，并保存新用户
        memberEntity.setCreateTime(new Date());
        memberEntity.setIntegration(0);
        memberEntity.setGrowth(0);
        memberEntity.setStatus(1);
        this.save(memberEntity);

        //4、新增玩用户后要及时删除手机验证码


    }

    @Override
    public MemberEntity queryMemberByNameAndPassword(String username, String password) {


        //先根据用户的名字进行查询，再用password再次确认
        MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>().eq("username", username));
        if (memberEntity == null) {
                throw new MemberException("用户名有误");
        }

        //要对password进行，比对，就要得到salt,进行解密
        String salt = memberEntity.getSalt();
        String s = DigestUtils.md5Hex(password + salt);
        //md5对于同一个密码用同样的salt加盐得到的密码是一样的
        if (!StringUtils.equals(memberEntity.getPassword(), s)) {
            throw new MemberException("密码有误");
        }
        return memberEntity;
    }

}