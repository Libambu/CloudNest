package com.yulong.easypan.entity.enums;

public enum UserStatusEnum {
    DISABLE(0,"禁用"),
    ENABLE(1,"启用");

    private Integer Status;
    private String desc;

    UserStatusEnum(Integer status,String desc){
        this.Status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return Status;
    }

    public String getDesc() {
        return desc;
    }
}
