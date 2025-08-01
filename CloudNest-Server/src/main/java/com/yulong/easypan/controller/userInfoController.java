package com.yulong.easypan.controller;


import com.yulong.easypan.annotation.GlobalInterceptor;
import com.yulong.easypan.component.RedisComponent;
import com.yulong.easypan.config.appConfig;
import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.CreateImageCode;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.dto.UserSpaceDto;
import com.yulong.easypan.entity.pjo.UserInfo;
import com.yulong.easypan.entity.vo.ResponseVO;
import com.yulong.easypan.exception.BusinessException;
import com.yulong.easypan.service.EmailCodeService;
import com.yulong.easypan.service.UserInfoService;
import com.yulong.easypan.utils.StringTools;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.Response;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
@Api(tags = "登录部分")
@Slf4j
public class userInfoController extends ABaseController{

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private appConfig appConfig;
    @Autowired
    private RedisComponent redisComponent;

    @GetMapping("/checkCode")
    @GlobalInterceptor(checklogin = false)
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
    @GlobalInterceptor(checklogin = false)
    public ResponseVO sendEmailCode(HttpSession httpSession, String email, String checkCode, Integer type){
        //在开发中需要对每个参数判定是否为null，每个函数都要写，所以统一的写一个AOP参数拦截
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
    @PostMapping("/register")
    @ApiOperation("注册功能")
    @GlobalInterceptor(checklogin = false)
    public ResponseVO register(HttpSession httpSession, String email, String nickName, String password, String checkCode, String emailCode){
        //在开发中需要对每个参数判定是否为null，每个函数都要写，所以统一的写一个AOP参数拦截
        try{
            if(!checkCode.equalsIgnoreCase((String)httpSession.getAttribute(Constants.CHECK_CODE_KEY))){
                throw  new BusinessException("图片验证码错误");
            }
            userInfoService.register(email,nickName,password,emailCode);
            return getSuccessResponseVO(null);
        }finally {
            httpSession.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    @PostMapping("/login")
    @GlobalInterceptor(checklogin = false)
    @ApiOperation("登录功能")
    public ResponseVO login(HttpSession httpSession,String email,String password,String checkCode){
        //在开发中需要对每个参数判定是否为null，每个函数都要写，所以统一的写一个AOP参数拦截
        try{
            if(!checkCode.equalsIgnoreCase((String)httpSession.getAttribute(Constants.CHECK_CODE_KEY))){
                throw  new BusinessException("图片验证码错误");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email,password);
            httpSession.setAttribute(Constants.SESSION_KEY,sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        }finally {
            httpSession.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    @PostMapping("resetPwd")
    @GlobalInterceptor(checklogin = false)
    @ApiOperation("重置密码")
    public ResponseVO resetPwd(HttpSession httpSession,String email,String password,String checkCode,String emailCode){
        try{
            if(!checkCode.equalsIgnoreCase((String) httpSession.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.resetPwd(email,password,emailCode);
            return getSuccessResponseVO(null);
        }finally {
            httpSession.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 用户头像api用于用户获取头像信息
     * @param httpSession
     * @param userId
     * @return
     */
    @GetMapping("/getAvatar/{userId}")
    @ApiOperation("获取用户头像")
    @GlobalInterceptor(checklogin = true)
    public void getAvatar(HttpServletResponse response,HttpSession httpSession,@PathVariable("userId") String userId){
        String avatarFolderName = appConfig.getBasePath() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File fileFold = new File(avatarFolderName);
        if(!fileFold.exists()){
            fileFold.mkdirs();
        }
        //userId的头像路径
        String avatarPath = avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File avatar = new File(avatarPath);
        //如果头像不存在启用默认头像
        if(!avatar.exists()){
            if(!(new File(avatarFolderName + Constants.AVATAR_DEFAULT).exists())){
                //如果默认头像不存在
                printNoDefaultImage(response);
            }
            String default_avatar = avatarFolderName + Constants.AVATAR_DEFAULT;
            response.setContentType("image/jpg");
            readFile(response,default_avatar);
        }
    }

    private void printNoDefaultImage(HttpServletResponse response){
        response.setHeader(CONTENT_TYPE,CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try{
            writer  = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        }catch (Exception e){
            log.error("输出默认图失败",e);
        }finally {
            writer.close();
        }
    }

    @GetMapping("/getUserInfo")
    @GlobalInterceptor
    @ApiOperation("获取用户信息")
    public ResponseVO getUserINfo(HttpSession session){
        SessionWebUserDto sessionWebUserDto = getUserInfoSession(session);
        return getSuccessResponseVO(sessionWebUserDto);
    }

    @GetMapping("/getUseSpace")
    @GlobalInterceptor
    @ApiOperation("获取使用空间")
    public ResponseVO getUseSpace(HttpSession session){
        SessionWebUserDto sessionWebUserDto = getUserInfoSession(session);
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceDto(sessionWebUserDto.getUserId());
        return getSuccessResponseVO(userSpaceDto);
    }

    @GetMapping("/logout")
    @GlobalInterceptor
    @ApiOperation("退出登录")
    public ResponseVO logout(HttpSession session){
        //销毁当前这个 HttpSession 实例。
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    @PostMapping("/updateUserAvatar")
    @GlobalInterceptor
    @ApiOperation("更新头像")
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar) {
        SessionWebUserDto webUserDto = getUserInfoSession(session);
        String baseFolder = appConfig.getBasePath() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            log.error("上传头像失败", e);
        }
        //上传头像后就会取消QQ的头像，也就是说把数据库里QQ头像链接设置为空字符串
        //以后获取头像的逻辑：如果QQ头像有就拿QQ头像，否则用userid去文件夹里拿
        String QqAvatar = "";
        userInfoService.updateQqAvatarByUserId(QqAvatar, webUserDto.getUserId());
        webUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY, webUserDto);
        return getSuccessResponseVO(null);
    }
    @PostMapping("/updatePassword")
    @GlobalInterceptor
    @ApiOperation("登录后重置密码")
    public  ResponseVO updatePassword(HttpSession session,String password){
        String md5pwd = StringTools.encodeByMd5(password);
        String userid = session.getId();
        userInfoService.updatePwdByUserId(md5pwd,userid);
        return getSuccessResponseVO(null);
    }
}
