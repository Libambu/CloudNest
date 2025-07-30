package com.yulong.easypan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("appConfig")
public class appConfig {
    @Value("${spring.mail.username:}")
    private String sendUserName;

    @Value("${admin.emails:}")
    private String adminUserl;

    public String getSendUserName() {
        return sendUserName;
    }
    public String getAdminUserl(){
        return adminUserl;
    }

}
