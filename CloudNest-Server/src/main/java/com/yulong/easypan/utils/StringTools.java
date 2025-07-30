package com.yulong.easypan.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

public class StringTools {
    /**
     * 生成count位数随机数
     * @param count
     * @return
     */
    public static final String getRandomNumber(Integer count){
        //这里有错误
        /*
         * 第 2 个参数：letters  false -> 不要字母
         * 第 3 个参数：numbers  false -> 不要数字
         * 结果：只能从“非字母非数字”的字符里随机，于是出现各种符号/汉字/韩文
         */
        //return RandomStringUtils.random(count,false,false);
        return RandomStringUtils.randomNumeric(count);
    }

    /**
     * 密码加密
     * @param orignString
     * @return
     */
    public static  String encodeByMd5(String orignString){
        return orignString.isEmpty()?null: DigestUtils.md5Hex(orignString);
    }
}
