package org.example.springboot.util;


import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.entity.User;
import org.example.springboot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Component
public class JwtTokenUtils {
    private static UserService staticUserService;
    private static RedisUtil staticRedisUtil;
    private static ObjectMapper staticObjectMapper;
    
    @Resource
    private UserService userService;
    
    @Resource
    private RedisUtil redisUtil;
    
    @Resource
    private ObjectMapper objectMapper;
    
    public static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenUtils.class);
    
    public static final String USER_TOKEN_KEY_PREFIX = "user:token:";
    public static final String USER_ID_KEY_PREFIX = "user:id:";
    public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    public static final long TOKEN_EXPIRE = 2 * 60 * 60; // 2小时
    public static final int TOKEN_EXPIRE_HOURS = 2;
    
    @PostConstruct
    public void setServices() {
        staticUserService = userService;
        staticRedisUtil = redisUtil;
        staticObjectMapper = objectMapper;
    }
    
    public static String genToken(String userId, String sign) {
        String safeUserId = Objects.requireNonNull(userId, "userId must not be null");
        String safeSign = Objects.requireNonNull(sign, "jwt sign must not be null");
        String token = JWT.create()
                .withAudience(safeUserId)
                .withExpiresAt(DateUtil.offsetHour(new Date(), TOKEN_EXPIRE_HOURS))
                .sign(Algorithm.HMAC256(safeSign));
        
        // 将token和用户ID的映射关系存入Redis，方便后续查询和管理
        redisUtil().set(USER_TOKEN_KEY_PREFIX + token, safeUserId, TOKEN_EXPIRE);
        
        return token;
    }

    public static String resolveToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) && StringUtils.isNotBlank(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            String authorization = request.getHeader("Authorization");
            if (StringUtils.isNotBlank(authorization) && authorization.startsWith("Bearer ")) {
                token = authorization.substring("Bearer ".length());
            }
        }
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }
        return token;
    }

    public static void writeTokenCookie(HttpServletResponse response, String token, boolean secure) {
        if (response == null || StringUtils.isBlank(token)) {
            return;
        }
        String cookie = ACCESS_TOKEN_COOKIE + "=" + token
                + "; Max-Age=" + TOKEN_EXPIRE
                + "; Path=/"
                + "; HttpOnly"
                + (secure ? "; Secure" : "")
                + "; SameSite=Lax";
        response.addHeader("Set-Cookie", cookie);
    }

    public static void clearTokenCookie(HttpServletResponse response, boolean secure) {
        if (response == null) {
            return;
        }
        String cookie = ACCESS_TOKEN_COOKIE + "="
                + "; Max-Age=0"
                + "; Path=/"
                + "; HttpOnly"
                + (secure ? "; Secure" : "")
                + "; SameSite=Lax";
        response.addHeader("Set-Cookie", cookie);
    }
    
    public static User getCurrentUser() {
        String token = null;
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
                LOGGER.error("获取当前请求上下文失败");
                return null;
            }
            HttpServletRequest request = servletRequestAttributes.getRequest();
            token = resolveToken(request);
            if (StringUtils.isBlank(token)) {
                LOGGER.error("获取当前登录的token失败，token{}", token);
                return null;
            }

            // 首先尝试从Redis中获取token对应的用户ID
            String userId = (String) redisUtil().get(USER_TOKEN_KEY_PREFIX + token);
            
            // 如果Redis中没有，则从JWT中解析
            if (StringUtils.isBlank(userId)) {
                var decodedJwt = JWT.decode(token);
                Date expiresAt = decodedJwt.getExpiresAt();
                if (expiresAt != null && expiresAt.before(new Date())) {
                    LOGGER.warn("Token has expired, token: {}", token);
                    return null;
                }
                userId = decodedJwt.getAudience().get(0);
                if (StringUtils.isBlank(userId)) {
                    LOGGER.error("从token中解析用户ID失败，token: {}", token);
                    return null;
                }
                // 将解析到的用户ID存入Redis
                redisUtil().set(USER_TOKEN_KEY_PREFIX + token, Objects.requireNonNull(userId, "userId must not be null"), TOKEN_EXPIRE);
            }
            
            // 尝试从Redis中获取用户信息
            String userKey = USER_ID_KEY_PREFIX + userId;
            Object userObj = redisUtil().get(userKey);
            User user = null;
            
            if (userObj != null) {
                try {
                    // 处理不同类型的缓存数据
                    if (userObj instanceof User) {
                        // 直接是User对象
                        user = (User) userObj;
                    } else if (userObj instanceof Map) {
                        // 如果是Map，使用ObjectMapper进行转换
                        String jsonStr = staticObjectMapper.writeValueAsString(userObj);
                        user = staticObjectMapper.readValue(jsonStr, User.class);
                    } else {
                        // 其他类型，尝试通过JSON序列化和反序列化进行转换
                        String jsonStr = staticObjectMapper.writeValueAsString(userObj);
                        user = staticObjectMapper.readValue(jsonStr, User.class);
                    }
                } catch (Exception e) {
                    LOGGER.error("Redis缓存中的用户数据转换失败，将从数据库重新获取", e);
                    user = null; // 转换失败，重新从数据库获取
                }
            }
            
            // 如果Redis中没有用户信息或转换失败，则从数据库查询
            if (user == null) {
                user = staticUserService.getUserById(Long.valueOf(userId));
                if (user != null) {
                    // 将用户信息存入Redis
                    redisUtil().set(userKey, Objects.requireNonNull(user, "user must not be null"), TOKEN_EXPIRE);
                }
            }
            
            return user;
        } catch (Exception e) {
            LOGGER.error("获取当前用户信息失败，token: {}", token, e);
            return null;
        }
    }
    
    /**
     * 更新Redis中的用户信息
     * 
     * @param user 用户信息
     */
    public static void updateUserCache(User user) {
        if (user != null && user.getId() != null) {
            String userKey = USER_ID_KEY_PREFIX + user.getId();
            redisUtil().set(userKey, Objects.requireNonNull(user, "user must not be null"), TOKEN_EXPIRE);
        }
    }

    public static String refreshToken(String oldToken, User user) {
        if (user == null || user.getId() == null || StringUtils.isBlank(user.getPassword())) {
            return null;
        }
        String newToken = genToken(String.valueOf(user.getId()), user.getPassword());
        if (StringUtils.isNotBlank(oldToken)) {
            redisUtil().del(USER_TOKEN_KEY_PREFIX + oldToken);
        }
        updateUserCache(user);
        return newToken;
    }

    public static void renewTokenTtl(String token, String userId) {
        if (StringUtils.isBlank(token) || StringUtils.isBlank(userId)) {
            return;
        }
        redisUtil().set(USER_TOKEN_KEY_PREFIX + token, Objects.requireNonNull(userId, "userId must not be null"), TOKEN_EXPIRE);
        Object cachedUser = redisUtil().get(USER_ID_KEY_PREFIX + userId);
        if (cachedUser != null) {
            redisUtil().set(USER_ID_KEY_PREFIX + userId, cachedUser, TOKEN_EXPIRE);
        }
    }
    
    /**
     * 清除用户的缓存信息
     * 
     * @param token 用户token
     */
    public static void clearUserCache(String token) {
        if (StringUtils.isNotBlank(token)) {
            String userId = (String) redisUtil().get(USER_TOKEN_KEY_PREFIX + token);
            if (StringUtils.isNotBlank(userId)) {
                // 删除用户信息缓存
                redisUtil().del(USER_ID_KEY_PREFIX + userId);
            }
            // 删除token缓存
            redisUtil().del(USER_TOKEN_KEY_PREFIX + token);
        }
    }

    private static RedisUtil redisUtil() {
        return Objects.requireNonNull(staticRedisUtil, "RedisUtil has not been initialized");
    }
}
