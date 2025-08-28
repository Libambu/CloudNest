package com.yulong.easypan.controller;

import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.SessionShareDto;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.enums.ResponseCodeEnum;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.exception.BusinessException;
import com.yulong.easypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Slf4j
public class ABaseController {
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";

    protected SessionShareDto getSessionShareFromSession(HttpSession session, String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        return sessionShareDto;
    }

    protected <T> ResponseVO getSuccessResponseVO(T t){
        ResponseVO<T>responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUS_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setdata(t);
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
        vo.setdata(t);
        return vo;
    }
    protected <T> ResponseVO getServerErrorResponseVO(T t){
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUS_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        vo.setdata(t);
        return vo;
    }

    /**
     * 将本地文件输出给前端
     * @param response
     * @param filePath
     */
    protected void readFile(HttpServletResponse response, String filePath) {
        if (!StringTools.pathIsOk(filePath)) {
            return;
        }
        OutputStream out = null;
        FileInputStream in = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            log.error("读取文件异常", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error("IO异常", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("IO异常", e);
                }
            }
        }
    }

    protected SessionWebUserDto getUserInfoSession(HttpSession session){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        return sessionWebUserDto;
    }
}
