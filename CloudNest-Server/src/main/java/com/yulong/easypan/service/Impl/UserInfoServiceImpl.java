package com.yulong.easypan.service.Impl;

import com.yulong.easypan.component.RedisComponent;
import com.yulong.easypan.config.appConfig;
import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.dto.SysSettingsDTO;
import com.yulong.easypan.entity.dto.UserSpaceDto;
import com.yulong.easypan.entity.enums.UserStatusEnum;
import com.yulong.easypan.entity.pjo.EmailCode;
import com.yulong.easypan.entity.pjo.UserInfo;
import com.yulong.easypan.exception.BusinessException;
import com.yulong.easypan.mappers.EmailCodeMapper;
import com.yulong.easypan.mappers.userInfoMapper;
import com.yulong.easypan.service.EmailCodeService;
import com.yulong.easypan.service.UserInfoService;
import com.yulong.easypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Slf4j
@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private userInfoMapper userInfoMapper;
    @Autowired
    private EmailCodeService emailCodeService;
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private appConfig appconfig;

    /**
     * 用户注册操作,开启事务，防止disable邮箱验证码之后，但是insert出错
     * @param email
     * @param nickName
     * @param password
     * @param emailCode
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(String email, String nickName, String password, String emailCode) {
        UserInfo userInfo1 = userInfoMapper.selectByEmail(email);
        if(userInfo1 != null){
            throw new BusinessException("邮箱已存在");
        }
        UserInfo userInfo2 = userInfoMapper.selectBynickName(nickName);
        if(userInfo2 != null){
            throw new BusinessException("昵称已存在");
        }
        //校验邮箱验证码
        emailCodeService.checkCode(email,emailCode);
        //获取15位的随机数作为userid
        String user_id = StringTools.getRandomNumber(15);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user_id);
        userInfo.setNickName(nickName);
        userInfo.setEmail(email);
        userInfo.setPassword(StringTools.encodeByMd5(password));
        userInfo.setJoinTime(LocalDateTime.now());
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setUserSpace(0L);
        SysSettingsDTO sysSettingsDTO = redisComponent.getSysSettingsDTO();
        userInfo.setTotalSpace(sysSettingsDTO.getUserInitUserSpace()* Constants.MB);
        userInfoMapper.insert(userInfo);
    }

    /**
     * 实现登录接口service
     * @param email
     * @param password
     * @return
     */
    @Override
    public SessionWebUserDto login(String email, String password) {
        UserInfo userInfo =  userInfoMapper.selectByEmail(email);
        //前端传过来的密码就已经是md5了
        if(userInfo==null||!userInfo.getPassword().equals(password)){
            throw new BusinessException("账号或密码错误");
        }
        if(userInfo.getStatus().equals(UserStatusEnum.DISABLE.getStatus())){
            throw new BusinessException("该账号已被禁用");
        }
        LocalDateTime lastlogin = LocalDateTime.now();
        userInfoMapper.updatelogin(email,lastlogin);
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setUserId(userInfo.getUserId());
        sessionWebUserDto.setNickName(userInfo.getNickName());
        sessionWebUserDto.setAvatar(userInfo.getQqAvatar());
        if(ArrayUtils.contains(appconfig.getAdminUserl().split(","),email)){
            sessionWebUserDto.setIsAdmin(true);
        }else{
            sessionWebUserDto.setIsAdmin(false);
        }
        //用户空间
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setUseSpace(userInfo.getUserSpace());
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisComponent.saveUserSpaceUse(userInfo.getUserId(), userSpaceDto);
        return sessionWebUserDto;

    }
}
