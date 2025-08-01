package com.yulong.easypan.component;


import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.SysSettingsDTO;
import com.yulong.easypan.entity.dto.UserSpaceDto;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("redisComponent")
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils;

    public SysSettingsDTO getSysSettingsDTO(){
        SysSettingsDTO sysSettingsDTO =(SysSettingsDTO) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if(sysSettingsDTO == null){
            sysSettingsDTO = new SysSettingsDTO();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING,sysSettingsDTO);
        }
        return  sysSettingsDTO;
    }
    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto){
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE+userId,userSpaceDto,60*60*24);
    }

    public UserSpaceDto getUserSpaceDto(String userId){
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE+userId);
        if(userSpaceDto==null){
            userSpaceDto = new UserSpaceDto();
            //TODO 查询当前用户已经上传文件的大小
            userSpaceDto.setUseSpace(0L);
            userSpaceDto.setTotalSpace(getSysSettingsDTO().getUserInitUserSpace()*Constants.MB);
            saveUserSpaceUse(userId,userSpaceDto);
        }
        return userSpaceDto;
    }
}
