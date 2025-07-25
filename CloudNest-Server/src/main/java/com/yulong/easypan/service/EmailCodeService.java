package com.yulong.easypan.service;

import com.yulong.easypan.entity.pjo.UserInfo;
import org.springframework.stereotype.Service;

@Service
public interface EmailCodeService {
    UserInfo test(String s);
    void sendEmailCode(String email, Integer type);
}
