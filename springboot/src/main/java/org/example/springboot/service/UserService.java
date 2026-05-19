package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;

import org.example.springboot.annotation.RedisCache;
import org.example.springboot.entity.User;
import org.example.springboot.DTO.UserPasswordUpdateDTO;
import org.example.springboot.enumClass.AccountStatus;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.UserMapper;
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
        if (dbUser.getStatus().equals(AccountStatus.PENDING_REVIEW.getValue())) {
            throw new ServiceException("账号正在审核");
        }
        if (dbUser.getStatus().equals(AccountStatus.REVIEW_FAILED.getValue())) {
            throw new ServiceException("账号审核不通过，请修改个人信息");
        }
        if (!bCryptPasswordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }

        return generateLoginResponse(dbUser);
    }

    public User loginByEmail(User user) {
        User dbUser = getByEmail(user.getEmail());
        if (dbUser.getStatus().equals(AccountStatus.PENDING_REVIEW.getValue())) {
            throw new ServiceException("账号正在审核");
        }
        if (dbUser.getStatus().equals(AccountStatus.REVIEW_FAILED.getValue())) {
            throw new ServiceException("账号审核不通过，请修改个人信息");
        }
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
        if (dbUser.getStatus() != null && dbUser.getStatus().equals(AccountStatus.PENDING_REVIEW.getValue())) {
            throw new ServiceException("账号正在审核");
        }
        if (dbUser.getStatus() != null && dbUser.getStatus().equals(AccountStatus.REVIEW_FAILED.getValue())) {
            throw new ServiceException("账号审核未通过，请联系管理员");
        }
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            throw new ServiceException("账号已被禁用，请联系管理员");
        }
    }

    private String generatePhoneUsername(String phone) {
        String base = "u" + phone.substring(phone.length() - 4);
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
        userLoginInfo.put("loginTime", System.currentTimeMillis());
        userLoginInfo.put("ip", loginIp);
        userLoginInfo.put("port", loginPort);

        redisUtil.hset(onlineUserKey, dbUser.getId().toString(), dbUser.getUsername());
        redisUtil.set(userInfoKey, userLoginInfo, USER_CACHE_EXPIRE);

        return dbUser;
    }

    @RedisCache(prefix = "user", key = "'role:' + #roleCode", expire = 3600)
    public List<User> getUserByRole(String roleCode) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRoleCode, roleCode));
        if (users.isEmpty()) {
            throw new ServiceException("未找到该角色的用户");
        }
        return users;
    }

    public void createUser(User user) {
        String registerIp = getClientIp();
        int registerPort = getClientPort();
        user.setRegisterIp(registerIp);
        user.setRegisterPort(registerPort);
        user.setLoginCount(0);

        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) != null) {
            throw new ServiceException("用户名已存在");
        }

        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, user.getEmail())) != null) {
            throw new ServiceException("邮箱已被使用");
        }

        user.setPassword(StringUtils.isNotBlank(user.getPassword()) ? user.getPassword() : DEFAULT_PWD);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));

        if (userMapper.insert(user) <= 0) {
            throw new ServiceException("用户创建失败");
        }

        logger.info("用户注册成功，用户ID: {}, 用户名: {}, 注册IP: {}, 端口: {}",
                user.getId(), user.getUsername(), registerIp, registerPort);

        redisUtil.del(USER_LIST_CACHE_KEY);
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
    }

    public void updateUser(Long id, User user) {
        if (userMapper.selectById(id) == null) {
            throw new ServiceException("要更新的用户不存在");
        }

        if (user.getUsername() != null) {
            User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
            if (existUser != null && !existUser.getId().equals(id)) {
                throw new ServiceException("新用户名已被使用");
            }
        }

        user.setId(id);
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("用户更新失败");
        }

        redisUtil.del(USER_CACHE_PREFIX + id);
        if (user.getUsername() != null) {
            redisUtil.del(USER_CACHE_PREFIX + "username:" + user.getUsername());
        }
        if (user.getEmail() != null) {
            redisUtil.del(USER_CACHE_PREFIX + "email:" + user.getEmail());
        }
        redisUtil.del(USER_LIST_CACHE_KEY);
        if (user.getRoleCode() != null) {
            redisUtil.del(USER_CACHE_PREFIX + "role:" + user.getRoleCode());
        }
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
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
        if (userMapper.deleteByIds(ids) <= 0) {
            throw new ServiceException("批量删除失败");
        }

        for (Integer id : ids) {
            redisUtil.del(USER_CACHE_PREFIX + id);
        }
        redisUtil.del(USER_LIST_CACHE_KEY);
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
        redisUtil.delByPrefix(USER_CACHE_PREFIX + "role:");
    }

    @RedisCache(prefix = "user", key = "'list'", expire = 3600)
    public List<User> getUserList() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());
        if (users.isEmpty()) {
            throw new ServiceException("未找到任何用户");
        }
        return users;
    }

    @RedisCache(prefix = "user", key = "#id", expire = 3600)
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    @RedisCache(prefix = "user:page", key = "#username + ':' + #sex + ':' + #nickname + ':' + #roleCode + ':' + #currentPage + ':' + #size", expire = 3600)
    public Page<User> getUsersByPage(String username, String sex, String nickname, String roleCode, Integer currentPage, Integer size) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(User::getUsername, username);
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
        return userMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }

    public void updatePassword(Long id, UserPasswordUpdateDTO update) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        if (!bCryptPasswordEncoder.matches(update.getOldPassword(), user.getPassword())) {
            throw new ServiceException("原密码错误");
        }

        user.setPassword(bCryptPasswordEncoder.encode(update.getNewPassword()));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码修改失败");
        }

        redisUtil.del(USER_CACHE_PREFIX + id);
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
        if (userMapper.deleteById(id) <= 0) {
            throw new ServiceException("删除失败");
        }

        redisUtil.del(USER_CACHE_PREFIX + id);
        redisUtil.del(USER_LIST_CACHE_KEY);
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
    }

    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
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
