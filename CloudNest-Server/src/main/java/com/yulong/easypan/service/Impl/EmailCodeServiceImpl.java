package com.yulong.easypan.service.Impl;

import com.yulong.easypan.component.RedisComponent;
import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.dto.SysSettingsDTO;
import com.yulong.easypan.entity.pjo.EmailCode;
import com.yulong.easypan.entity.pjo.UserInfo;
import com.yulong.easypan.exception.BusinessException;
import com.yulong.easypan.mappers.EmailCodeMapper;
import com.yulong.easypan.mappers.userInfoMapper;
import com.yulong.easypan.service.EmailCodeService;
import com.yulong.easypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Slf4j
@Service
public class EmailCodeServiceImpl implements EmailCodeService {

    @Autowired
    private userInfoMapper userInfoMapper;

    @Autowired
    private EmailCodeMapper emailCodeMapper;

    @Resource
    private JavaMailSender javaMailSender;
    @Autowired
    private com.yulong.easypan.config.appConfig appConfig;
    @Resource
    private RedisComponent redisComponent;


    /**
     * 发送邮箱验证码service
     * @param email
     * @param type
     */
    @Override
    //开启事务
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, Integer type) {
        if(type==Constants.ZERO){
            UserInfo userInfo = userInfoMapper.selectByEmail(email);
            if(userInfo!=null){
                throw new BusinessException("邮箱已经存储存在");
            }
        }
        String Code = StringTools.getRandomNumber(Constants.LENGTH_5);
        log.info("验证码为"+Code);
        //TODO 向对方邮件发送验证码
        sendMailCode(email,Code);
        //将之前发的验证码置为无效
        emailCodeMapper.disableEmailCode(email);
        EmailCode emailCode = new EmailCode(Code,email,LocalDateTime.now(),Constants.ZERO);
        emailCodeMapper.insert(emailCode);
    }

    /**
     * 用于注册检验邮箱验证码
     * @param email
     * @param Code
     */
    @Override
    public void checkCode(String email, String Code) {
        EmailCode emailCode = emailCodeMapper.SelectCodeByEmailANDCode(email,Code);
        if(emailCode == null){
            throw new BusinessException("邮箱验证码不正确");
        }
        LocalDateTime t = emailCode.getCreateTime();
        long creattime = t.toInstant(ZoneOffset.of("+8")).toEpochMilli();
        if (emailCode.getStatus() == 1 || System.currentTimeMillis()-creattime>=15*60*1000){
            throw  new BusinessException("验证码已失效");
        }
        emailCodeMapper.disableEmailCode(email);

    }

    /**
     * 用于发送邮箱验证码
     * @param tomail
     * @param Code
     */
    private void sendMailCode(String tomail,String Code){
        try{
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true);
            helper.setFrom(appConfig.getSendUserName());
            helper.setTo(tomail);
            SysSettingsDTO sysSettingsDTO = redisComponent.getSysSettingsDTO();
            helper.setSubject(sysSettingsDTO.getRegisterMailTitle());
            helper.setText(String.format(sysSettingsDTO.getRegisterEmailContent(),Code));
            helper.setSentDate(new Date());
            javaMailSender.send(message);
        }catch (Exception e){
            log.info("邮件发送失败",e);
            throw  new BusinessException("邮件发送失败");
        }
    }
}
