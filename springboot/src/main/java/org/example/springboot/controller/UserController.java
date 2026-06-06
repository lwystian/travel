package org.example.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.common.Result;
import org.example.springboot.dto.EmailBindCodeDTO;
import org.example.springboot.dto.EmailBindConfirmDTO;
import org.example.springboot.dto.ForgotPasswordDTO;
import org.example.springboot.dto.PhoneChangeCodeDTO;
import org.example.springboot.dto.PhoneChangeConfirmDTO;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.dto.UserPasswordUpdateDTO;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.service.EmailService;
import org.example.springboot.service.AuthConfigService;
import org.example.springboot.service.GeetestCaptchaService;
import org.example.springboot.service.SmsCodeService;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.security.RolePermission;
import org.example.springboot.security.SecurityGuards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Tag(name="用户管理接口")
@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    @Resource
    private UserService userService;
    @Autowired
    private UserMapper userMapper;
    @Resource
    private EmailService emailService;
    @Resource
    private SmsCodeService smsCodeService;
    @Resource
    private AuthConfigService authConfigService;
    @Resource
    private GeetestCaptchaService geetestCaptchaService;

    @Operation(summary = "根据id获取用户信息")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        if (!currentUser.getId().equals(id)) {
            SecurityGuards.requirePermission("user:view");
        }
        User user = userService.getUserById(id);
        return Result.success(userService.attachRoleInfo(user));
    }

    @Operation(summary = "根据username获取用户信息")
    @GetMapping("/username/{username}")
    public Result<?> getUserByUsername(@PathVariable String username) {
        SecurityGuards.requirePermission("user:view");
        User user = userService.getByUsername(username);
        return Result.success(userService.attachRoleInfo(user));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
        User loginUser = userService.login(user);
        writeLoginCookie(loginUser, request, response);
        return Result.success(loginUser);
    }

    @Operation(summary = "邮箱登录")
    @PostMapping("/login/email")
    public Result<?> loginByEmail(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
        if (!StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("邮箱和密码不能为空");
        }
        User loginUser = userService.loginByEmail(user);
        writeLoginCookie(loginUser, request, response);
        return Result.success(loginUser);
    }

    @Operation(summary = "密码修改")
    @PutMapping("/password/{id}")
    public Result<?> updatePassword(@PathVariable Long id, @RequestBody UserPasswordUpdateDTO userPasswordUpdate) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(id)) {
            throw new ServiceException("只能修改当前登录账号的密码");
        }
        userService.updatePassword(id, userPasswordUpdate);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "忘记密码")
    @PostMapping("/forget")
    public Result<?> forgetPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        if (!emailService.verifyCode(dto.getEmail(), "RESET_PASSWORD", dto.getCode())) {
            throw new ServiceException("邮箱验证码错误或已过期");
        }
        userService.forgetPassword(dto.getEmail(), dto.getNewPassword());
        return Result.success("密码重置成功");
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<?> getUsersByPage(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String phone,
            @RequestParam(defaultValue = "") String sex,
            @RequestParam(defaultValue = "") String nickname,
            @RequestParam(defaultValue = "") String roleCode,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        SecurityGuards.requirePermission("user:view");
        Page<User> page = userService.getUsersByPage(username, phone, sex, nickname, roleCode, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "根据角色获取用户列表")
    @GetMapping("/role/{roleCode}")
    public Result<?> getUserByRole(@PathVariable String roleCode) {
        SecurityGuards.requirePermission("user:view");
        List<User> users = userService.getUserByRole(roleCode);
        users.forEach(userService::attachRoleInfo);
        return Result.success(users);
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam List<Integer> ids) {
        User currentUser = SecurityGuards.requirePermission("user:delete");
        for (Integer id : ids) {
            userService.deleteUserById(Long.valueOf(id), currentUser);
        }
        return Result.success("批量删除成功");
    }

    @Operation(summary = "获取所有用户")
    @GetMapping("/all")
    public Result<?> getUserList() {
        SecurityGuards.requirePermission("user:view");
        List<User> list = userService.getUserList();
        list.forEach(userService::attachRoleInfo);
        return Result.success(list);
    }

    @Operation(summary = "创建新用户")
    @PostMapping("/add")
    public Result<?> createUser(@RequestBody  User user) {
        User currentUser = SecurityGuards.requirePermission("user:create");
        userService.createUser(user, currentUser);
        return Result.success("创建成功");
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        if (currentUser.getId().equals(id)) {
            User updated = userService.updateOwnProfile(id, user);
            return Result.success("个人信息更新成功", userService.attachRoleInfo(updated));
        }
        SecurityGuards.requirePermission("user:update");
        userService.updateUser(id, user, currentUser);
        return Result.success("更新成功");
    }

    @Operation(summary = "更新当前用户基础资料")
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody User user) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        User updated = userService.updateOwnProfile(currentUser.getId(), user);
        return Result.success("个人信息更新成功", userService.attachRoleInfo(updated));
    }

    @Operation(summary = "发送邮箱绑定验证码")
    @PostMapping("/email/bind/code")
    public Result<?> sendEmailBindCode(@RequestBody EmailBindCodeDTO dto) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        if (userService.existsByEmail(dto.getEmail())) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail().trim().toLowerCase()));
            if (user != null && !user.getId().equals(currentUser.getId())) {
                throw new ServiceException("该邮箱已被其他账号绑定");
            }
        }
        geetestCaptchaService.verify(authConfigService.getGeetestConfigForVerify(), dto.getGeetest());
        emailService.sendVerificationCodeAsync(dto.getEmail());
        return Result.success("邮箱验证码已发送，请注意查收");
    }

    @Operation(summary = "确认绑定邮箱")
    @PostMapping("/email/bind/confirm")
    public Result<?> confirmEmailBind(@RequestBody EmailBindConfirmDTO dto) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        if (!emailService.verifyCode(dto.getEmail(), "EMAIL_BIND", dto.getCode())) {
            throw new ServiceException("邮箱验证码错误或已过期");
        }
        userService.bindEmail(currentUser.getId(), dto.getEmail());
        return Result.success("邮箱绑定成功");
    }

    @Operation(summary = "发送手机号变更验证码")
    @PostMapping("/phone/change/code")
    public Result<?> sendPhoneChangeCode(@RequestBody PhoneChangeCodeDTO dto) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        smsCodeService.validatePhone(dto.getPhone());
        User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (existUser != null && !existUser.getId().equals(currentUser.getId())) {
            throw new ServiceException("该手机号已被其他账号绑定");
        }
        geetestCaptchaService.verify(authConfigService.getGeetestConfigForVerify(), dto.getGeetest());
        smsCodeService.sendCode(dto.getPhone(), "CHANGE_PHONE", userSubject(currentUser.getId()));
        return Result.success("短信验证码已发送，请注意查收");
    }

    @Operation(summary = "发送当前绑定手机号验证码")
    @PostMapping("/phone/current/code")
    public Result<?> sendCurrentPhoneCode(@RequestBody PhoneChangeCodeDTO dto) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        User latest = userService.getUserById(currentUser.getId());
        if (!StringUtils.hasText(latest.getPhone())) {
            throw new ServiceException("当前账号未绑定手机号");
        }
        geetestCaptchaService.verify(authConfigService.getGeetestConfigForVerify(), dto.getGeetest());
        smsCodeService.sendCode(latest.getPhone(), "VERIFY_CURRENT", userSubject(currentUser.getId()));
        return Result.success("当前手机号验证码已发送，请注意查收");
    }

    @Operation(summary = "确认变更手机号")
    @PostMapping("/phone/change/confirm")
    public Result<?> confirmPhoneChange(@RequestBody PhoneChangeConfirmDTO dto) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        User latest = userService.getUserById(currentUser.getId());
        if (RolePermission.isSuperAdmin(latest)) {
            if (!StringUtils.hasText(latest.getPhone())) {
                throw new ServiceException("超级管理员必须先绑定手机号");
            }
            smsCodeService.verifyCode(latest.getPhone(), "VERIFY_CURRENT", dto.getCurrentSmsCode(), userSubject(currentUser.getId()));
        }
        smsCodeService.verifyCode(dto.getPhone(), "CHANGE_PHONE", dto.getSmsCode(), userSubject(currentUser.getId()));
        userService.changePhone(currentUser.getId(), dto.getPhone());
        return Result.success("手机号变更成功");
    }

    private String userSubject(Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        return "USER:" + userId;
    }

    @Operation(summary = "根据id删除用户")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteUserById(@PathVariable Long id) {
        User currentUser = SecurityGuards.requirePermission("user:delete");
        userService.deleteUserById(id, currentUser);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<?> getCurrentUser() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }
        return Result.success(userService.attachRoleInfo(currentUser));
    }
    @Operation(summary = "修改用户状态")
    @PutMapping("/status/{userId}")
    public Result<?> updateStatus(@PathVariable Long userId, @RequestParam Integer status) {
        User currentUser = SecurityGuards.requirePermission("user:update");
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        user.setStatus(status);
        userService.updateUser(userId, user, currentUser);
        return Result.success();

    }

    @Operation(summary = "设置管理员订单通知")
    @PutMapping({"/{id}/order-notify", "/{id}/order-sms-notify"})
    public Result<?> updateOrderNotify(@PathVariable Long id, @RequestParam Boolean enabled) {
        User currentUser = SecurityGuards.requireSuperAdmin();
        userService.updateOrderNotifyEnabled(id, enabled, currentUser);
        return Result.success(Boolean.TRUE.equals(enabled) ? "已开启订单通知" : "已关闭订单通知");
    }

    @Operation(summary = "管理员重置用户密码")
    @PutMapping("/resetPassword/{id}")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody UserPasswordUpdateDTO dto) {
        User currentUser = SecurityGuards.requirePermission("user:reset-password");
        userService.resetPassword(id, dto.getNewPassword(), currentUser);
        return Result.success("重置密码成功");
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = JwtTokenUtils.resolveToken(request);
        
        userService.logout(token);
        JwtTokenUtils.clearTokenCookie(response, isSecureRequest(request));
        return Result.success("登出成功");
    }
    
    @Operation(summary = "获取在线用户数量")
    @GetMapping("/online/count")
    public Result<?> getOnlineUserCount() {
        long count = userService.getOnlineUserCount();
        return Result.success(count);
    }
    
    @Operation(summary = "获取在线用户列表")
    @GetMapping("/online/list")
    public Result<?> getOnlineUserList() {
        SecurityGuards.requirePermission("log:view");
        List<Map<String, Object>> onlineUsers = userService.getOnlineUserList();
        return Result.success(onlineUsers);
    }

    @Operation(summary = "手机号注册")
    @PostMapping("/register/phone")
    public Result<?> registerByPhone(@RequestBody User user, @RequestParam String verifyCode) {
        // 验证参数
        if (user == null || !StringUtils.hasText(user.getPhone()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("手机号和密码不能为空");
        }
        
        try {
            smsCodeService.verifyCode(user.getPhone(), "REGISTER", verifyCode);

            // 生成默认用户名
            if (!StringUtils.hasText(user.getUsername())) {
                user.setUsername("user_" + user.getPhone().substring(user.getPhone().length() - 4));
            }
            
            // 设置默认角色
            user.setRoleCode("USER");
            
            // 创建用户
            userService.createUser(user);
            return Result.success("注册成功");
        } catch (Exception e) {
            LOGGER.error("手机号注册失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @Operation(summary = "邮箱验证码注册")
    @PostMapping("/register/email")
    public Result<?> registerByEmail(@RequestBody User user, @RequestParam String verifyCode) {
        // 验证参数
        if (user == null || !StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("邮箱和密码不能为空");
        }
        
        try {
            // 验证验证码
            boolean isValid = emailService.verifyCode(user.getEmail(), verifyCode);
            if (!isValid) {
                return Result.error("验证码错误或已过期");
            }
            
            // 生成默认用户名
            if (!StringUtils.hasText(user.getUsername())) {
                String username = generateUniqueUsername(user);
                user.setUsername(username);
            }
            
            // 设置默认角色
            user.setRoleCode("USER");
            
            // 创建用户
            userService.createUser(user);
            return Result.success("注册成功");
        } catch (Exception e) {
            LOGGER.error("邮箱注册失败", e);
            return Result.error(e.getMessage());
        }
    }


    // 定义一个方法用于生成随机字母
    public  String getRandomLetters() {
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            int index = random.nextInt(letters.length());
            sb.append(letters.charAt(index));
        }
        return sb.toString();
    }

    // 生成默认用户名，确保不重复
    public  String generateUniqueUsername(User user) {
        String username = user.getUsername();
        if (!StringUtils.hasText(username)) {
            String emailPrefix = user.getEmail().split("@")[0];
            // 初始化随机部分
            String randomSuffix = getRandomLetters();
            // 生成初始用户名
            String candidateUsername = emailPrefix + randomSuffix;
            // 检查是否重复，如果重复则循环生成新的直到不重复
            while (isUsernameExist(candidateUsername)) {
                randomSuffix = getRandomLetters();
                candidateUsername = emailPrefix + randomSuffix;
            }
            user.setUsername(candidateUsername);
            return candidateUsername;
        } else {
            // 如果已有用户名，检查是否重复，重复则按照相同逻辑生成
            if (isUsernameExist(username)) {
                String emailPrefix = user.getEmail().split("@")[0];
                String randomSuffix = getRandomLetters();
                String candidateUsername = emailPrefix + randomSuffix;
                while (isUsernameExist(candidateUsername)) {
                    randomSuffix = getRandomLetters();
                    candidateUsername = emailPrefix + randomSuffix;
                }
                user.setUsername(candidateUsername);
                return candidateUsername;
            } else {
                return username;
            }
        }
    }

    // 检查用户名是否在数据库中存在
    public  boolean isUsernameExist(String username) {
        // 实现数据库查询逻辑，返回是否存在该用户名

        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername,username))>0;
    }

    private void writeLoginCookie(User user, HttpServletRequest request, HttpServletResponse response) {
        if (user == null || !StringUtils.hasText(user.getToken())) {
            return;
        }
        JwtTokenUtils.writeTokenCookie(response, user.getToken(), isSecureRequest(request));
        JwtTokenUtils.writeTokenExpireHeader(response);
        user.setToken(null);
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
    }
}
