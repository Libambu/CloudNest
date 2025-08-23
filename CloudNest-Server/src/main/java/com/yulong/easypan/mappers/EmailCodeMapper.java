package com.yulong.easypan.mappers;

import com.yulong.easypan.entity.pjo.EmailCode;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailCodeMapper {
    @Insert("insert into easypan.email_code value (#{email},#{code},#{createTime},#{status})")
    void insert(EmailCode emailCode);

    void disableEmailCode(String email);

    EmailCode SelectCodeByEmailANDCode(String email,String Code);
}
