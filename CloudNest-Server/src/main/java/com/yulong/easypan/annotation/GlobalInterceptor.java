package com.yulong.easypan.annotation;


import org.apache.ibatis.annotations.Mapper;

import java.lang.annotation.*;

/**
 * 自定义注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapper
public @interface GlobalInterceptor {

    /**
     * 校验登录
     * @return
     */
    boolean checklogin() default true;

    /**
     * 校验超级管理员
     * @return
     */
    boolean checkAdmin() default false;
}
