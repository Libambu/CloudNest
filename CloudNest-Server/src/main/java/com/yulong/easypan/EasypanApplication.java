package com.yulong.easypan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableAsync
@EnableTransactionManagement
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.yulong.easypan"})
@MapperScan(basePackages = {"com.yulong.easypan.mappers"})
public class EasypanApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasypanApplication.class, args);
    }
}
