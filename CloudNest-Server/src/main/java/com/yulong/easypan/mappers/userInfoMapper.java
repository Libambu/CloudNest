package com.yulong.easypan.mappers;


import com.yulong.easypan.entity.pjo.UserInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface userInfoMapper {
    public UserInfo selectByEmail(String email);
}
