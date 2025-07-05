package com.yulong.easypan.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "测试一下的接口")
public class test {

    @ApiOperation(value = "test")
    @GetMapping("/test")
    public int test(){

        return 2;
    }
    
}
