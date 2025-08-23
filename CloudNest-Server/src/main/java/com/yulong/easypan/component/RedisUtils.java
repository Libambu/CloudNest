package com.yulong.easypan.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("redisUtils")
public class RedisUtils<V>{
    @Resource
    private RedisTemplate<String,V> redisTemplate;

    public V get(String key){
        return key==null?null:redisTemplate.opsForValue().get(key);
    }
    public boolean set(String key,V value){
        try{
            redisTemplate.opsForValue().set(key,value);
            return true;
        }catch (Exception e){
            log.info("设置redis失败{}，{}",key,value);
            return false;
        }
    }
    public boolean setex(String key,V value,long time){
        try{
            if(time>0){
                redisTemplate.opsForValue().set(key,value,time, TimeUnit.SECONDS);
            }else{
                set(key,value);
            }
            return true;
        }catch (Exception e){
            log.info("设置redis失败");
            return false;
        }
    }
}
