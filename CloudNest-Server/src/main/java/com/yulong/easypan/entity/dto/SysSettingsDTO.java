package com.yulong.easypan.entity.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

//用于反序列化时忽略 JSON 中多余的字段。
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SysSettingsDTO implements Serializable {
    private String registerMailTitle = "邮箱验证码";
    private String registerEmailContent = "您好，您的验证码是：%s,15分钟内有效";
    private Integer userInitUserSpace = 5;
}
