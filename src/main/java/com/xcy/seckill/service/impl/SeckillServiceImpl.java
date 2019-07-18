package com.xcy.seckill.service.impl;

import com.xcy.seckill.mapper.SeckillMapper;
import com.xcy.seckill.pojo.DataResult;
import com.xcy.seckill.pojo.Seckill;
import com.xcy.seckill.pojo.SuccessKilled;
import com.xcy.seckill.service.SeckillService;
import com.xcy.seckill.utils.JedisClient;
import com.xcy.seckill.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    SeckillMapper seckillMapper;

    @Autowired
    JedisClient jedisClient;

    @Override
    public List<Seckill> getSeckillList() {
        return seckillMapper.getSeckillList();
    }

    @Override
    public Seckill getSeckillById(long id) {
        //此处需要添加一个redis缓存
        boolean isExists = jedisClient.exists("SECKILL:"+id);
        Seckill seckill = null;
        if(isExists){
            System.out.println("从缓存中查找数据");
            String jsonStr = jedisClient.get("SECKILL:"+id);
            seckill = JsonUtils.jsonToPojo(jsonStr,Seckill.class);
        }else{
            System.out.println("从数据库中查找数据");
            seckill=  seckillMapper.getSeckillById(id);
            jedisClient.set("SECKILL:"+id,JsonUtils.objectToJson(seckill));
        }
        return seckill;
    }

    @Override
    @Transactional
    public DataResult execSecKillHandler(SuccessKilled successKilled) {
        //插入新的数据
        int count = seckillMapper.insertSuccessKilled(successKilled);
        if(count > 0){
            int yxLine= seckillMapper.jianKuCun(successKilled.getSeckillId());
            if(yxLine <= 0){
                //此处需要抛异常，事务回滚
                return new DataResult(3,"秒杀异常");
            }else{
                return new DataResult(0,"秒杀成功");
            }
        }else{
            return new DataResult(2,"重复秒杀");
        }
    }
}
