package com.yulong.easypan.entity.pjo;

import com.fasterxml.jackson.annotation.JsonFormat;


import java.time.LocalDateTime;

public class EmailCode {
    private String code;
    private String email;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private Integer status;

    public EmailCode(String Code, String Email, LocalDateTime lds,Integer status){
        this.code = Code;
        this.email = Email;
        this.createTime = lds;
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        code = code;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        createTime = createTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        status = status;
    }
}
