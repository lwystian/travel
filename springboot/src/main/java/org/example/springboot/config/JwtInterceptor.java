package org.example.springboot.config;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.entity.User;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;




@Component
public class JwtInterceptor implements HandlerInterceptor {
    public static final Logger LOGGER = LoggerFactory.getLogger(HandlerInterceptor.class);
    private static final long REFRESH_THRESHOLD_SECONDS = 6 * 60 * 60;
    
    // 将用户ID存储到请求属性中的key
    public static final String USER_ID_ATTRIBUTE = "userId";
    
    // Redis中token的key前缀
    private static final String USER_TOKEN_KEY_PREFIX = "user:token:";
    
    @Resource
    private UserService userService;
    
    @Resource
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,  HttpServletResponse response,  Object handler) throws Exception {
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }
        if (StringUtils.isBlank(token)) {
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                return true;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401状态码
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"code\":\"401\",\"msg\":\"请先登录\"}"); // 返回JSON错误信息
            return false;
        }

        // 首先检查Redis中token是否还存在，如果不存在说明已过期
        String redisUserId = (String) redisUtil.get(USER_TOKEN_KEY_PREFIX + token);
        if (StringUtils.isBlank(redisUserId)) {
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                return true;
            }
            LOGGER.warn("Token已在Redis中过期或不存在, token={}", token);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"code\":\"401\",\"msg\":\"登录已过期，请重新登录\"}");
            return false;
        }

        User user;
        try {
            String userId = JWT.decode(token).getAudience().get(0);
            user = userService.getUserById(Long.valueOf(userId));
            // 将用户ID存储到请求属性中
            request.setAttribute(USER_ID_ATTRIBUTE, Long.valueOf(userId));
        } catch (Exception e) {
            String errMsg = "token失效，请重新登录";
            LOGGER.error(errMsg + " ,token=" + token, e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"code\":\"401\",\"msg\":\"" + errMsg + "\"}");
            return false;
        }
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"code\":\"401\",\"msg\":\"用户不存在，请重新登录\"}");
            return false;
        }
        try {
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
            jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"code\":\"401\",\"msg\":\"登录验证失败，请重新登录\"}");
            return false;
        }
        renewTokenIfNeeded(token, redisUserId, user, response);
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    private void renewTokenIfNeeded(String token, String userId, User user, HttpServletResponse response) {
        JwtTokenUtils.renewTokenTtl(token, userId);
        try {
            long expiresAt = JWT.decode(token).getExpiresAt().getTime();
            long remainingSeconds = (expiresAt - System.currentTimeMillis()) / 1000;
            response.setHeader("X-Token-Expire", String.valueOf(System.currentTimeMillis() + JwtTokenUtils.TOKEN_EXPIRE * 1000));
            response.setHeader("Access-Control-Expose-Headers", "X-Refresh-Token,X-Token-Expire");
            if (remainingSeconds > 0 && remainingSeconds <= REFRESH_THRESHOLD_SECONDS) {
                String newToken = JwtTokenUtils.refreshToken(token, user);
                if (StringUtils.isNotBlank(newToken)) {
                    response.setHeader("X-Refresh-Token", newToken);
                    response.setHeader("X-Token-Expire", String.valueOf(System.currentTimeMillis() + JwtTokenUtils.TOKEN_EXPIRE * 1000));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("刷新token有效期失败", e);
        }
    }
}
