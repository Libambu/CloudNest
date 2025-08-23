package com.yulong.easypan.controller;

import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.exception.BusinessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHander extends ABaseController{
    /**
     * 自定义异常处理
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseVO handle(Exception e){
        if(e instanceof BusinessException){
            return getBusinessErrorResponseVO((BusinessException) e,null);
        }
        return getServerErrorResponseVO(null);
    }
}
