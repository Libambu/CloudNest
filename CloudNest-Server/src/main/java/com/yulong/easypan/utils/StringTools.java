package com.yulong.easypan.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

public class StringTools {
    /**
     * 生成count位数随机数
     * @param count
     * @return
     */
    public static final String getRandomNumber(Integer count){
        return RandomStringUtils.random(count,false,false);
    }
}
