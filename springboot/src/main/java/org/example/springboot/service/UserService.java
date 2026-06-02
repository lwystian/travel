package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;

import org.example.springboot.annotation.RedisCache;
import org.example.springboot.entity.User;
import org.example.springboot.dto.UserPasswordUpdateDTO;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.RolePermission;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final String USER_CACHE_PREFIX = "user:";
    private static final String USER_LIST_CACHE_KEY = "user:list";
    private static final String USER_PAGE_CACHE_PREFIX = "user:page:";
    private static final long USER_CACHE_EXPIRE = 3600; // 缓存1小时

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisUtil redisUtil;

    @Value("${user.defaultPassword}")
    private String DEFAULT_PWD;

    @Resource
    private PasswordEncoder bCryptPasswordEncoder;

    @Resource
    private SiteNotificationService siteNotificationService;

    @Resource
    private SmsCodeService smsCodeService;

    public User attachRoleInfo(User user) {
        if (user == null) {
            return null;
        }
        String roleCode = RolePermission.normalizeRole(user.getRoleCode());
        user.setRoleCode(roleCode);
        user.setRoleName(RolePermission.roleNameOf(roleCode));
        user.setPermissions(RolePermission.permissionsOf(roleCode));
        user.setSuperAdmin(RolePermission.SUPER_ADMIN.equals(roleCode));
        user.setProtectedAccount(Boolean.TRUE.equals(user.getSuperAdmin()));
        user.setPassword(null);
        return user;
    }

    @RedisCache(prefix = "user", key = "'email:' + #email", expire = 3600)
    public User getByEmail(String email) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }
        return user;
    }

    public User login(User user) {
        User dbUser = getByUsername(user.getUsername());
        validateLoginStatus(dbUser);
        if (!bCryptPasswordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }

        return generateLoginResponse(dbUser);
    }

    public User loginByEmail(User user) {
        User dbUser = getByEmail(user.getEmail());
        validateLoginStatus(dbUser);
        if (!bCryptPasswordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new ServiceException("邮箱或密码错误");
        }

        return generateLoginResponse(dbUser);
    }

    public User loginByPhonePassword(String phone, String password) {
        if (StringUtils.isBlank(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("请输入正确的手机号");
        }
        if (StringUtils.isBlank(password)) {
            throw new ServiceException("请输入密码");
        }
        User dbUser = getByPhone(phone);
        validateLoginStatus(dbUser);
        if (!bCryptPasswordEncoder.matches(password, dbUser.getPassword())) {
            throw new ServiceException("手机号或密码错误");
        }
        return generateLoginResponse(dbUser);
    }

    public User loginByPhoneCode(String phone) {
        if (StringUtils.isBlank(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("请输入正确的手机号");
        }
        User dbUser = getByPhone(phone);
        validateLoginStatus(dbUser);
        return generateLoginResponse(dbUser);
    }

    public boolean existsByPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return false;
        }
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0;
    }

    public boolean existsByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email.trim().toLowerCase())) > 0;
    }

    public User registerByPhone(String phone, String password) {
        if (StringUtils.isBlank(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("请输入正确的手机号");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) != null) {
            throw new ServiceException("该手机号已注册，请直接登录");
        }

        String registerIp = getClientIp();
        int registerPort = getClientPort();
        User user = new User();
        user.setPhone(phone);
        user.setUsername(generatePhoneUsername(phone));
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setRoleCode("USER");
        user.setStatus(1);
        user.setLoginCount(0);
        user.setRegisterIp(registerIp);
        user.setRegisterPort(registerPort);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        if (userMapper.insert(user) <= 0) {
            throw new ServiceException("注册失败，请稍后再试");
        }

        redisUtil.del(USER_LIST_CACHE_KEY);
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
        siteNotificationService.sendToUser(user.getId(), "注册成功",
                "欢迎加入侠客行国旅，你可以浏览景点、发布攻略、收藏行程并管理订单。",
                "ACCOUNT", "REGISTER", String.valueOf(user.getId()), "/profile");
        return user;
    }

    private void validateLoginStatus(User dbUser) {
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            throw new ServiceException("账号已被禁用，请联系管理员");
        }
    }

    private String generatePhoneUsername(String phone) {
        String base = "user" + phone.substring(phone.length() - 4);
        String username = base;
        int index = 1;
        while (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            username = base + index++;
        }
        return username;
    }

    private User generateLoginResponse(User dbUser) {
        String loginIp = getClientIp();
        int loginPort = getClientPort();

        dbUser.setLastLoginIp(loginIp);
        dbUser.setLastLoginTime(LocalDateTime.now());
        dbUser.setLoginCount(dbUser.getLoginCount() == null ? 1 : dbUser.getLoginCount() + 1);
        userMapper.updateById(dbUser);

        String token = JwtTokenUtils.genToken(String.valueOf(dbUser.getId()), dbUser.getPassword());
        dbUser.setToken(token);

        String userCacheKey = USER_CACHE_PREFIX + dbUser.getId();
        redisUtil.set(userCacheKey, dbUser, USER_CACHE_EXPIRE);
        logger.info("用户登录信息已缓存，key: {}, IP: {}, 登录次数: {}", userCacheKey, loginIp, dbUser.getLoginCount());

        String onlineUserKey = "online:users";
        String userInfoKey = "online:info:" + dbUser.getId();

        Map<String, Object> userLoginInfo = new HashMap<>();
        userLoginInfo.put("userId", dbUser.getId());
        userLoginInfo.put("username", dbUser.getUsername());
        userLoginInfo.put("nickname", dbUser.getNickname());
        userLoginInfo.put("roleCode", dbUser.getRoleCode());
        userLoginInfo.put("permissions", RolePermission.permissionsOf(dbUser.getRoleCode()));
        userLoginInfo.put("loginTime", System.currentTimeMillis());
        userLoginInfo.put("ip", loginIp);
        userLoginInfo.put("port", loginPort);

        String onlineUserId = String.valueOf(dbUser.getId());
        String onlineUsername = StringUtils.isNotBlank(dbUser.getUsername()) ? dbUser.getUsername() : onlineUserId;
        redisUtil.hset(
                Objects.requireNonNull(onlineUserKey, "online user key must not be null"),
                Objects.requireNonNull(onlineUserId, "online user id must not be null"),
                Objects.requireNonNull(onlineUsername, "online username must not be null")
        );
        redisUtil.set(userInfoKey, userLoginInfo, USER_CACHE_EXPIRE);

        return attachRoleInfo(dbUser);
    }

    public List<User> getUserByRole(String roleCode) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRoleCode, roleCode));
        if (users.isEmpty()) {
            throw new ServiceException("未找到该角色的用户");
        }
        return users;
    }

    public void createUser(User user) {
        normalizeOptionalFields(user);
        user.setRoleCode(RolePermission.USER);
        createUserInternal(user);
    }

    public void createUser(User user, User actor) {
        requireAdminActor(actor);
        normalizeOptionalFields(user);
        user.setRoleCode(RolePermission.normalizeRole(user.getRoleCode()));
        if (!RolePermission.canManageRole(actor, user.getRoleCode())) {
            throw new ServiceException("无权创建该角色用户");
        }
        createUserInternal(user);
    }

    private void createUserInternal(User user) {
        if (RolePermission.SUPER_ADMIN.equals(RolePermission.normalizeRole(user.getRoleCode()))
                && countSuperAdminAccounts() > 0) {
            throw new ServiceException("系统只能存在一个超级管理员");
        }
        String registerIp = getClientIp();
        int registerPort = getClientPort();
        user.setRegisterIp(registerIp);
        user.setRegisterPort(registerPort);
        user.setLoginCount(0);

        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) != null) {
            throw new ServiceException("用户名已存在");
        }

        if (StringUtils.isNotBlank(user.getEmail())
                && userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, user.getEmail())) != null) {
            throw new ServiceException("邮箱已被使用");
        }
        if (StringUtils.isNotBlank(user.getPhone())
                && userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, user.getPhone())) != null) {
            throw new ServiceException("手机号已被使用");
        }

        user.setPassword(StringUtils.isNotBlank(user.getPassword()) ? user.getPassword() : DEFAULT_PWD);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setRoleCode(StringUtils.isNotBlank(user.getRoleCode()) ? user.getRoleCode() : RolePermission.USER);
        user.setOrderNotifyEnabled(RolePermission.isSuperAdmin(user));
        user.setStatus(user.getStatus() == null ? 1 : user.getStatus());
        user.setCreateTime(user.getCreateTime() == null ? LocalDateTime.now() : user.getCreateTime());
        user.setUpdateTime(LocalDateTime.now());

        if (userMapper.insert(user) <= 0) {
            throw new ServiceException("用户创建失败");
        }

        logger.info("用户注册成功，用户ID: {}, 用户名: {}, 注册IP: {}, 端口: {}",
                user.getId(), user.getUsername(), registerIp, registerPort);

        redisUtil.del(USER_LIST_CACHE_KEY);
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
    }

    public void updateUser(Long id, User user) {
        normalizeOptionalFields(user);
        User oldUser = userMapper.selectById(id);
        if (oldUser == null) {
            throw new ServiceException("要更新的用户不存在");
        }
        updateUserInternal(id, user, oldUser);
    }

    public void updateUser(Long id, User user, User actor) {
        requireAdminActor(actor);
        normalizeOptionalFields(user);
        User oldUser = userMapper.selectById(id);
        if (oldUser == null) {
            throw new ServiceException("要更新的用户不存在");
        }
        assertCanManageTarget(actor, oldUser);
        String requestedRole = user.getRoleCode() == null ? oldUser.getRoleCode() : RolePermission.normalizeRole(user.getRoleCode());
        if (!RolePermission.canManageRole(actor, requestedRole)) {
            throw new ServiceException("无权设置该用户角色");
        }
        user.setRoleCode(requestedRole);
        updateUserInternal(id, user, oldUser);
    }

    private void updateUserInternal(Long id, User user, User oldUser) {
        user.setOrderNotifyEnabled(null);
        String requestedRole = user.getRoleCode() == null ? oldUser.getRoleCode() : RolePermission.normalizeRole(user.getRoleCode());
        if (RolePermission.SUPER_ADMIN.equals(requestedRole)
                && !RolePermission.isSuperAdmin(oldUser)
                && countSuperAdminAccounts() > 0) {
            throw new ServiceException("系统只能存在一个超级管理员");
        }
        if (RolePermission.isSuperAdmin(oldUser) && !RolePermission.SUPER_ADMIN.equals(requestedRole)) {
            throw new ServiceException("超级管理员是系统唯一根账号，不能降权");
        }
        if (RolePermission.isSuperAdmin(oldUser) && user.getStatus() != null && user.getStatus() == 0) {
            throw new ServiceException("不能禁用超级管理员");
        }
        user.setRoleCode(requestedRole);
        if (!RolePermission.isSuperAdmin(oldUser) && RolePermission.SUPER_ADMIN.equals(requestedRole)) {
            user.setOrderNotifyEnabled(true);
        }

        if (user.getUsername() != null) {
            User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
            if (existUser != null && !existUser.getId().equals(id)) {
                throw new ServiceException("新用户名已被使用");
            }
        }
        if (StringUtils.isNotBlank(user.getEmail())) {
            User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, user.getEmail()));
            if (existUser != null && !existUser.getId().equals(id)) {
                throw new ServiceException("新邮箱已被使用");
            }
        }
        if (StringUtils.isNotBlank(user.getPhone())) {
            User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, user.getPhone()));
            if (existUser != null && !existUser.getId().equals(id)) {
                throw new ServiceException("新手机号已被使用");
            }
        }

        user.setId(id);
        user.setUpdateTime(LocalDateTime.now());
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("用户更新失败");
        }

        clearUserCache(id, oldUser, user);
    }

    public void updateOrderNotifyEnabled(Long id, Boolean enabled, User actor) {
        if (!RolePermission.isSuperAdmin(actor)) {
            throw new ServiceException("只有超级管理员可以配置订单通知");
        }
        if (id == null) {
            throw new ServiceException("用户ID不能为空");
        }
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new ServiceException("用户不存在");
        }
        String roleCode = RolePermission.normalizeRole(target.getRoleCode());
        if (!RolePermission.SUPER_ADMIN.equals(roleCode) && !RolePermission.ADMIN.equals(roleCode)) {
            throw new ServiceException("只能为管理员账号配置订单通知");
        }
        if (Boolean.TRUE.equals(enabled)) {
            if (!Integer.valueOf(1).equals(target.getStatus())) {
                throw new ServiceException("请先启用该管理员账号");
            }
            if (!hasOrderNotificationContact(target)) {
                throw new ServiceException("该管理员未绑定有效邮箱或手机号，无法接收订单通知");
            }
        }

        User update = new User();
        update.setId(id);
        update.setOrderNotifyEnabled(Boolean.TRUE.equals(enabled));
        update.setUpdateTime(LocalDateTime.now());
        if (userMapper.updateById(update) <= 0) {
            throw new ServiceException("订单通知配置失败");
        }
        clearUserCache(id, target, update);
    }

    private boolean hasOrderNotificationContact(User user) {
        return user != null && (isValidNotifyEmail(user.getEmail()) || isValidNotifyPhone(user.getPhone()));
    }

    private boolean isValidNotifyEmail(String email) {
        return StringUtils.isNotBlank(email)
                && email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidNotifyPhone(String phone) {
        return StringUtils.isNotBlank(phone) && phone.trim().matches("^1[3-9]\\d{9}$");
    }

    public User updateOwnProfile(Long id, User profile) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        if (StringUtils.isNotBlank(profile.getNickname())) {
            user.setNickname(profile.getNickname().trim());
        }
        if (StringUtils.isNotBlank(profile.getSex())) {
            if (!"男".equals(profile.getSex()) && !"女".equals(profile.getSex())) {
                throw new ServiceException("性别参数不正确");
            }
            user.setSex(profile.getSex());
        }
        if (StringUtils.isNotBlank(profile.getAvatar())) {
            user.setAvatar(profile.getAvatar());
        }
        user.setUpdateTime(LocalDateTime.now());
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("个人信息更新失败");
        }
        clearUserCache(id, user, user);
        siteNotificationService.sendToUser(id, "资料已更新",
                "你的个人基本资料刚刚完成更新。",
                "ACCOUNT", "PROFILE", String.valueOf(id), "/profile");
        return user;
    }

    public void bindEmail(Long id, String email) {
        if (StringUtils.isBlank(email) || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ServiceException("请输入正确的邮箱地址");
        }
        User oldUser = userMapper.selectById(id);
        if (oldUser == null) {
            throw new ServiceException("用户不存在");
        }
        String normalizedEmail = email.trim().toLowerCase();
        User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, normalizedEmail));
        if (existUser != null && !existUser.getId().equals(id)) {
            throw new ServiceException("该邮箱已被其他账号绑定");
        }
        User user = new User();
        user.setId(id);
        user.setEmail(normalizedEmail);
        user.setUpdateTime(LocalDateTime.now());
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("邮箱绑定失败");
        }
        clearUserCache(id, oldUser, user);
        siteNotificationService.sendToUser(id, "邮箱已绑定",
                "你的账号邮箱刚刚完成验证绑定，如非本人操作请立即联系管理员。",
                "ACCOUNT", "EMAIL", String.valueOf(id), "/profile");
    }

    public void changePhone(Long id, String phone) {
        if (StringUtils.isBlank(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("请输入正确的手机号");
        }
        User oldUser = userMapper.selectById(id);
        if (oldUser == null) {
            throw new ServiceException("用户不存在");
        }
        User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (existUser != null && !existUser.getId().equals(id)) {
            throw new ServiceException("该手机号已被其他账号绑定");
        }
        User user = new User();
        user.setId(id);
        user.setPhone(phone);
        user.setUpdateTime(LocalDateTime.now());
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("手机号变更失败");
        }
        clearUserCache(id, oldUser, user);
        siteNotificationService.sendToUser(id, "手机号已变更",
                "你的登录手机号刚刚完成验证变更，如非本人操作请立即联系管理员。",
                "ACCOUNT", "PHONE", String.valueOf(id), "/profile");
    }

    private void clearUserCache(Long id, User oldUser, User newUser) {
        redisUtil.del(USER_CACHE_PREFIX + id);
        redisUtil.del(JwtTokenUtils.USER_ID_KEY_PREFIX + id);
        if (oldUser != null) {
            if (oldUser.getUsername() != null) {
                redisUtil.del(USER_CACHE_PREFIX + "username:" + oldUser.getUsername());
            }
            if (oldUser.getEmail() != null) {
                redisUtil.del(USER_CACHE_PREFIX + "email:" + oldUser.getEmail());
            }
            if (oldUser.getPhone() != null) {
                redisUtil.del(USER_CACHE_PREFIX + "phone:" + oldUser.getPhone());
            }
            if (oldUser.getRoleCode() != null) {
                redisUtil.del(USER_CACHE_PREFIX + "role:" + oldUser.getRoleCode());
            }
        }
        if (newUser != null) {
            if (newUser.getUsername() != null) {
                redisUtil.del(USER_CACHE_PREFIX + "username:" + newUser.getUsername());
            }
            if (newUser.getEmail() != null) {
                redisUtil.del(USER_CACHE_PREFIX + "email:" + newUser.getEmail());
            }
            if (newUser.getPhone() != null) {
                redisUtil.del(USER_CACHE_PREFIX + "phone:" + newUser.getPhone());
            }
            if (newUser.getRoleCode() != null) {
                redisUtil.del(USER_CACHE_PREFIX + "role:" + newUser.getRoleCode());
            }
            User refreshed = userMapper.selectById(id);
            if (refreshed != null) {
                JwtTokenUtils.updateUserCache(refreshed);
            }
        }
        redisUtil.del(USER_LIST_CACHE_KEY);
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
    }

    private void normalizeOptionalFields(User user) {
        if (user == null) {
            return;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            user.setEmail(null);
        }
        if (StringUtils.isBlank(user.getPhone())) {
            user.setPhone(null);
        }
    }

    @RedisCache(prefix = "user", key = "'username:' + #username", expire = 3600)
    public User getByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择需要删除的用户");
        }
        for (Integer id : ids) {
            deleteUserById(Long.valueOf(id));
        }
    }

    public List<User> getUserList() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());
        if (users.isEmpty()) {
            throw new ServiceException("未找到任何用户");
        }
        return users;
    }

    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    public Page<User> getUsersByPage(String username, String phone, String sex, String nickname, String roleCode, Integer currentPage, Integer size) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(User::getUsername, username);
        }
        if (StringUtils.isNotBlank(phone)) {
            queryWrapper.like(User::getPhone, phone);
        }
        if (StringUtils.isNotBlank(sex)) {
            queryWrapper.eq(User::getSex, sex);
        }
        if (StringUtils.isNotBlank(nickname)) {
            queryWrapper.like(User::getNickname, nickname);
        }
        if (StringUtils.isNotBlank(roleCode)) {
            queryWrapper.eq(User::getRoleCode, roleCode);
        }
        queryWrapper.last("ORDER BY CASE WHEN role_code = 'SUPER_ADMIN' THEN 0 WHEN role_code = 'ADMIN' THEN 1 ELSE 2 END, id DESC");
        Page<User> page = userMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        page.getRecords().forEach(this::attachRoleInfo);
        return page;
    }

    public void updatePassword(Long id, UserPasswordUpdateDTO update) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        if (!bCryptPasswordEncoder.matches(update.getOldPassword(), user.getPassword())) {
            throw new ServiceException("原密码错误");
        }
        if (StringUtils.isBlank(user.getPhone())) {
            throw new ServiceException("请先绑定手机号后再修改密码");
        }
        smsCodeService.verifyCode(user.getPhone(), "VERIFY_CURRENT", update.getCurrentPhoneCode(), "USER:" + id);

        user.setPassword(bCryptPasswordEncoder.encode(update.getNewPassword()));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码修改失败");
        }

        redisUtil.del(USER_CACHE_PREFIX + id);
        redisUtil.del(JwtTokenUtils.USER_ID_KEY_PREFIX + id);
        siteNotificationService.sendToUser(id, "密码已修改",
                "你的账号密码刚刚完成修改，如非本人操作请立即联系管理员。",
                "ACCOUNT", "PASSWORD", String.valueOf(id), "/profile");
    }

    public void forgetPassword(String email, String newPassword) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码重置失败");
        }

        redisUtil.del(USER_CACHE_PREFIX + user.getId());
        redisUtil.del(USER_CACHE_PREFIX + "email:" + email);
        if (user.getUsername() != null) {
            redisUtil.del(USER_CACHE_PREFIX + "username:" + user.getUsername());
        }
        siteNotificationService.sendToUser(user.getId(), "密码已重置",
                "你的账号密码已通过找回密码流程重置，请使用新密码登录。",
                "ACCOUNT", "PASSWORD", String.valueOf(user.getId()), "/login");
    }

    public void deleteUserById(Long id) {
        deleteUserById(id, null);
    }

    public void deleteUserById(Long id, User actor) {
        requireAdminActor(actor);
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new ServiceException("用户不存在");
        }
        if (actor != null && actor.getId() != null && actor.getId().equals(id)) {
            throw new ServiceException("不能删除当前登录账号");
        }
        assertCanManageTarget(actor, target);
        if (RolePermission.isSuperAdmin(target)) {
            throw new ServiceException("超级管理员是系统唯一根账号，不能删除");
        }
        target.setStatus(0);
        target.setUpdateTime(LocalDateTime.now());
        if (userMapper.updateById(target) <= 0) {
            throw new ServiceException("账号停用失败");
        }

        redisUtil.del(USER_CACHE_PREFIX + id);
        redisUtil.del(JwtTokenUtils.USER_ID_KEY_PREFIX + id);
        redisUtil.del(USER_LIST_CACHE_KEY);
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
        redisUtil.delByPrefix(USER_CACHE_PREFIX + "role:");
    }

    public void resetPassword(Long id, String newPassword) {
        resetPassword(id, newPassword, null);
    }

    public void resetPassword(Long id, String newPassword, User actor) {
        requireAdminActor(actor);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        assertCanManageTarget(actor, user);
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码重置失败");
        }

        redisUtil.del(USER_CACHE_PREFIX + id);
        siteNotificationService.sendToUser(id, "管理员已重置你的密码",
                "管理员已为你的账号重置密码，请使用新密码登录并妥善保管。",
                "ACCOUNT", "PASSWORD", String.valueOf(id), "/login");
    }

    public List<User> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(ids);
    }

    public long countSuperAdmins() {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRoleCode, RolePermission.SUPER_ADMIN)
                .eq(User::getStatus, 1));
    }

    public long countSuperAdminAccounts() {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRoleCode, RolePermission.SUPER_ADMIN));
    }

    public long countPrivilegedUsers() {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .in(User::getRoleCode, List.of(RolePermission.SUPER_ADMIN, RolePermission.ADMIN))
                .eq(User::getStatus, 1));
    }

    private void requireAdminActor(User actor) {
        if (actor == null) {
            return;
        }
        if (!RolePermission.isAdmin(actor)) {
            throw new ServiceException("无权限");
        }
    }

    private void assertCanManageTarget(User actor, User target) {
        if (actor == null || target == null) {
            return;
        }
        if (RolePermission.isSuperAdmin(actor)) {
            return;
        }
        if (RolePermission.isSuperAdmin(target) || RolePermission.ADMIN.equals(RolePermission.normalizeRole(target.getRoleCode()))) {
            throw new ServiceException("管理员不能管理超级管理员或其他管理员");
        }
    }

    @RedisCache(prefix = "user", key = "'phone:' + #phone", expire = 3600)
    public User getByPhone(String phone) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new ServiceException("手机号不存在");
        }
        return user;
    }

    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }

        try {
            User currentUser = JwtTokenUtils.getCurrentUser();
            if (currentUser != null) {
                redisUtil.del(USER_CACHE_PREFIX + currentUser.getId());
                String onlineUserKey = "online:users";
                String userInfoKey = "online:info:" + currentUser.getId();
                redisUtil.hdel(onlineUserKey, currentUser.getId().toString());
                redisUtil.del(userInfoKey);
                JwtTokenUtils.clearUserCache(token);
                logger.info("用户登出成功，用户ID: {}", currentUser.getId());
            }
        } catch (Exception e) {
            logger.error("用户登出异常", e);
        }
    }

    public long getOnlineUserCount() {
        return redisUtil.hsize("online:users");
    }

    public List<Map<String, Object>> getOnlineUserList() {
        Map<Object, Object> onlineUsers = redisUtil.hmget("online:users");
        List<Map<String, Object>> resultList = new ArrayList<>();
        if (onlineUsers != null && !onlineUsers.isEmpty()) {
            for (Map.Entry<Object, Object> entry : onlineUsers.entrySet()) {
                String userInfoKey = "online:info:" + entry.getKey();
                Object userInfo = redisUtil.get(userInfoKey);
                if (userInfo instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userInfoMap = (Map<String, Object>) userInfo;
                    resultList.add(userInfoMap);
                }
            }
        }
        return resultList;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                String ip = requestAttributes.getRequest().getHeader("X-Forwarded-For");
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getHeader("Proxy-Client-IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getHeader("WL-Proxy-Client-IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getHeader("HTTP_CLIENT_IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getRemoteAddr();
                }
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                return ip;
            }
            return "unknown";
        } catch (Exception e) {
            logger.error("获取客户端IP异常", e);
            return "unknown";
        }
    }

    private int getClientPort() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                return requestAttributes.getRequest().getRemotePort();
            }
            return 0;
        } catch (Exception e) {
            logger.error("获取客户端端口异常", e);
            return 0;
        }
    }
}
