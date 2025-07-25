package com.yulong.easypan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("appConfig")
public class appConfig {
    @Value("${spring.mail.username:}")
    private String sendUserName;

    public String getSendUserName() {
        return sendUserName;
    }
}
