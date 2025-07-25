package com.yulong.easypan.entity.pjo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Version;

import java.io.Serializable;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class UserInfo implements Serializable {


    /**
     * 主键
     */

    private String userId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * QQ openID
     */
    private String qqOpenId;

    /**
     * QQ头像
     */
    private String qqAvatar;

    /**
     * 密码
     */
    private String password;

    /**
     * 加入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinTime;

    /**
     * 最后一次登陆时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 用户状态，0:禁用 1:启用
     */
    private Integer status;

    /**
     * 使用空间 单位 byte
     */
    private Long userSpace;

    /**
     * 总空间
     */
    private Long totalSpace;



}

