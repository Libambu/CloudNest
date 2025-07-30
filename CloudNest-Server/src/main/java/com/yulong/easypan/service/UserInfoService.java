package com.yulong.easypan.service;

import com.yulong.easypan.entity.dto.SessionWebUserDto;

public interface UserInfoService {
    /**
     * 实现注册
     * @param email
     * @param nickName
     * @param password
     * @param emailCode
     */
    void register(String email,String nickName,String password,String emailCode);
    SessionWebUserDto login(String email, String password);
}
