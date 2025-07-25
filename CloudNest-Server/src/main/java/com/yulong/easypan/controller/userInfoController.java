package com.yulong.easypan.controller;


import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.CreateImageCode;
import com.yulong.easypan.entity.pjo.UserInfo;
import com.yulong.easypan.entity.result.Result;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.exception.BusinessException;
import com.yulong.easypan.service.EmailCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@RestController
@Api(tags = "登录部分")
public class userInfoController extends ABaseController{
    @Autowired
    private EmailCodeService emailCodeService;




    @GetMapping("/checkCode")
    @ApiOperation("传递验证码")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException, IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    /**
     *
     * @param httpSession
     * @param email
     * @param checkCode
     * @param type ==0为注册 1为找回密码
     * @return
     */
    @PostMapping("/sendEmailCode")
    @ApiOperation("向邮箱发送验证码")
    public ResponseVO sendEmailCode(HttpSession httpSession, String email, String checkCode, Integer type){
        try{
            if(!checkCode.equalsIgnoreCase((String)httpSession.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))){
                throw  new BusinessException("图片验证码错误");
            }
            emailCodeService.sendEmailCode(email,type);
            return getSuccessResponseVO(null);
        }finally {
            httpSession.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

}
