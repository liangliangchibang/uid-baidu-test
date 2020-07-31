package com.baidu.mydemo.controller;

import com.baidu.mydemo.service.UidGenService;
import com.baidu.mydemo.snowflake.SnowflakeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class UidController {
    @Autowired
    private UidGenService uidGenService;

    @Autowired
    private SnowflakeService snowflakeService;


    /**
     * 测试使用
     *
     * @return java.lang.String
     * @date 2020/7/31 10:14
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 获得百度雪花算法id
     *
     * @param num
     * @return java.lang.String
     * @date 2020/7/31 10:00
     */
    @GetMapping({"/uidGenerator/{num}", "/uidGenerator/", "/uidGenerator"})
    public String UidGenerator(@PathVariable(value = "num", required = false) Integer num) {

        int i = Objects.isNull(num) ? 1 : num;
        return String.valueOf(uidGenService.getUid(i));
    }

    @GetMapping({"/uid/{num}", "/uid/", "/uid"})
    public String getUid(@PathVariable(value = "num", required = false) Integer num) {

        int i = Objects.isNull(num) ? 1 : num;
        return String.valueOf(snowflakeService.getUid(i));
    }


}
