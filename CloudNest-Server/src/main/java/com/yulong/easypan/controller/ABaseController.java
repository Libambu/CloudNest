package com.yulong.easypan.controller;

import com.yulong.easypan.entity.enums.ResponseCodeEnum;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.exception.BusinessException;

public class ABaseController {
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";

    protected <T> ResponseVO getSuccessResponseVO(T t){
        ResponseVO<T>responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUS_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setDate(t);
        return responseVO;
    }
    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t){
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUS_ERROR);
        if(e.getCode()==null){
            vo.setCode(ResponseCodeEnum.CODE_600.getCode());
        }else{
            vo.setCode(e.getCode());
        }
        vo.setInfo(e.getMessage());
        vo.setDate(t);
        return vo;
    }
    protected <T> ResponseVO getServerErrorResponseVO(T t){
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUS_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        vo.setDate(t);
        return vo;
    }
}
