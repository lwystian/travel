package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.dto.GeetestConfigDTO;
import org.example.springboot.dto.PhoneCodeLoginDTO;
import org.example.springboot.dto.PhonePasswordLoginDTO;
import org.example.springboot.dto.PhoneRegisterDTO;
import org.example.springboot.dto.SmsCodeRequestDTO;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.service.AuthConfigService;
import org.example.springboot.service.GeetestCaptchaService;
import org.example.springboot.service.PasswordCryptoService;
import org.example.springboot.service.SmsCodeService;
import org.example.springboot.service.UserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "手机号认证接口")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,32}$";

    @Resource
    private AuthConfigService authConfigService;

    @Resource
    private GeetestCaptchaService geetestCaptchaService;

    @Resource
    private SmsCodeService smsCodeService;

    @Resource
    private UserService userService;

    @Resource
    private PasswordCryptoService passwordCryptoService;

    @Operation(summary = "获取前端极验配置")
    @GetMapping("/geetest/public")
    public Result<GeetestConfigDTO> geetestPublicConfig() {
        GeetestConfigDTO config = authConfigService.getGeetestConfigForVerify();
        if (!Boolean.TRUE.equals(config.getEnabled()) || !Boolean.TRUE.equals(config.getConfigured())) {
            return Result.success(authConfigService.getGeetestPublicConfig());
        }
        return Result.success(geetestCaptchaService.register(config));
    }

    @Operation(summary = "获取登录密码加密公钥")
    @GetMapping("/crypto/public-key")
    public Result<Map<String, Object>> publicKey() {
        return Result.success(Map.of(
                "algorithm", "RSA-OAEP-256",
                "enabled", passwordCryptoService.isEncryptionEnabled(),
                "mode", passwordCryptoService.isEncryptionEnabled() ? "RSA_ENCRYPTED" : "PLAIN_DEV_ONLY",
                "publicKey", passwordCryptoService.getPublicKeyBase64()
        ));
    }

    @Operation(summary = "发送短信验证码")
    @OperationLog(operationType = "CREATE", description = "发送短信验证码", targetType = "登录注册")
    @PostMapping("/sms/send")
    public Result<?> sendSmsCode(@RequestBody SmsCodeRequestDTO dto) {
        smsCodeService.validatePhone(dto.getPhone());
        String scene = smsCodeService.normalizeScene(dto.getScene());
        if (!"REGISTER".equals(scene) && !"LOGIN".equals(scene)) {
            throw new ServiceException("该短信场景不允许从公共接口发送");
        }
        if ("REGISTER".equals(scene) && userService.existsByPhone(dto.getPhone())) {
            throw new ServiceException("该手机号已注册，请直接登录");
        }
        if ("LOGIN".equals(scene) && !userService.existsByPhone(dto.getPhone())) {
            throw new ServiceException("该手机号尚未注册，请先完成注册");
        }
        geetestCaptchaService.verify(authConfigService.getGeetestConfigForVerify(), dto.getGeetest());
        smsCodeService.sendCode(dto.getPhone(), scene);
        return Result.success("验证码已发送，请注意查收");
    }

    @Operation(summary = "手机号验证码注册")
    @OperationLog(operationType = "CREATE", description = "手机号注册", targetType = "用户账号")
    @PostMapping("/register/phone")
    public Result<User> registerByPhone(@RequestBody PhoneRegisterDTO dto) {
        requireAgreement(dto.getAgreementAccepted());
        String password = passwordCryptoService.decryptPassword(dto.getEncryptedPassword());
        String confirmPassword = passwordCryptoService.decryptPassword(dto.getEncryptedConfirmPassword());
        validatePassword(password, confirmPassword);
        smsCodeService.verifyCode(dto.getPhone(), "REGISTER", dto.getSmsCode());
        User user = userService.registerByPhone(dto.getPhone(), password);
        return Result.success("注册成功", user);
    }

    @Operation(summary = "手机号密码登录")
    @OperationLog(operationType = "LOGIN", description = "手机号密码登录", targetType = "用户账号", logParams = false)
    @PostMapping("/login/password")
    public Result<User> loginByPhonePassword(@RequestBody PhonePasswordLoginDTO dto) {
        requireAgreement(dto.getAgreementAccepted());
        String password = passwordCryptoService.decryptPassword(dto.getEncryptedPassword());
        User user = userService.loginByPhonePassword(dto.getPhone(), password);
        return Result.success("登录成功", user);
    }

    @Operation(summary = "手机号验证码登录")
    @OperationLog(operationType = "LOGIN", description = "手机号验证码登录", targetType = "用户账号", logParams = false)
    @PostMapping("/login/code")
    public Result<User> loginByPhoneCode(@RequestBody PhoneCodeLoginDTO dto) {
        requireAgreement(dto.getAgreementAccepted());
        smsCodeService.verifyCode(dto.getPhone(), "LOGIN", dto.getSmsCode());
        User user = userService.loginByPhoneCode(dto.getPhone());
        return Result.success("登录成功", user);
    }

    @Operation(summary = "获取用户协议")
    @GetMapping("/agreement")
    public Result<Map<String, Object>> agreement() {
        return Result.success(Map.of(
                "version", "2026.05",
                "title", "用户服务协议与隐私保护条款",
                "effectiveDate", "2026-05-20"
        ));
    }

    private void requireAgreement(Boolean accepted) {
        if (!Boolean.TRUE.equals(accepted)) {
            throw new ServiceException("请先阅读并同意用户服务协议与隐私保护条款");
        }
    }

    private void validatePassword(String password, String confirmPassword) {
        if (!StringUtils.hasText(password)) {
            throw new ServiceException("请输入密码");
        }
        if (!password.equals(confirmPassword)) {
            throw new ServiceException("两次输入的密码不一致");
        }
        if (!password.matches(PASSWORD_PATTERN)) {
            throw new ServiceException("密码需为8-32位，并同时包含大写字母、小写字母、数字和特殊符号");
        }
    }
}
