package com.yulong.easypan.service;

import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.pjo.UserInfo;
import org.springframework.stereotype.Service;

@Service
public interface EmailCodeService {
    void sendEmailCode(String email, Integer type);
    void checkCode(String email,String Code);

}
