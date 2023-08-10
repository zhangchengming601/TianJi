package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 签到Controller
 * */
@RestController
@RequestMapping("/sign-record")
@RequiredArgsConstructor
public class SignRecordController {

    private final ISignRecordService recordService;

    @PostMapping
    @ApiOperation("签到功能接口")
    public SignResultVO addSignRecords(){
        return recordService.addSignRecords();
    }

}
