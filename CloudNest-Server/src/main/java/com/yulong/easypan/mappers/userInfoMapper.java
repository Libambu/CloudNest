package com.yulong.easypan.mappers;


import com.yulong.easypan.entity.pjo.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface userInfoMapper {
    public UserInfo selectByEmail(String email);
    public UserInfo selectBynickName(String nickName);
    void insert(UserInfo userInfo);
    @Update("update easypan.user_info set last_login_time = #{lastLoginTime} where email=#{email}")
    void updatelogin(String email, LocalDateTime lastLoginTime);
}
