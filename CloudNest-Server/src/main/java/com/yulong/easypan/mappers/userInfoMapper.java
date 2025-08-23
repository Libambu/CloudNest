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

    @Update("update easypan.user_info set password = #{s} where email = #{email}")
    void resetPwd(String email, String s);

    @Update("update easypan.user_info set qq_avatar = #{qqAvatar} where user_id = #{userId}")
    void updateQqAvatarByUserId(String qqAvatar, String userId);

    @Update("update easypan.user_info set password = #{md5pwd} where user_id = #{userid}")
    void updatePwdByUserId(String md5pwd, String userid);

    Integer updateUserSpace(String userId, Long userSpace, Long totalSpace);
}
