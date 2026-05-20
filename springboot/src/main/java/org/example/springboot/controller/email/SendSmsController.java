package org.example.springboot.controller.email;

import org.example.springboot.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SendSmsController {

    @GetMapping({"/code/{phone}", "/verify"})
    public Result<?> legacySmsEndpointDisabled() {
        return Result.error("旧短信接口已停用，请使用统一认证短信接口");
    }
}
