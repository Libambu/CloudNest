package com.yulong.easypan.component;


import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.SysSettingsDTO;
import com.yulong.easypan.entity.dto.UserSpaceDto;
import com.yulong.easypan.mappers.FileInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("redisComponent")
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils;
    @Autowired
    private FileInfoMapper fileInfoMapper;

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
            // 查询当前用户已经上传文件的大小
            Long size = fileInfoMapper.selectUserSpace(userId);
            userSpaceDto.setUseSpace(size);
            userSpaceDto.setTotalSpace(Constants.MB* getSysSettingsDTO().getUserInitUserSpace());
            saveUserSpaceUse(userId,userSpaceDto);
        }
        return userSpaceDto;
    }
}
