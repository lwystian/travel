package org.example.springboot.config;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import jakarta.annotation.Resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.entity.User;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;




@Component
public class JwtInterceptor implements HandlerInterceptor {
    public static final Logger LOGGER = LoggerFactory.getLogger(HandlerInterceptor.class);
    private static final String[] PUBLIC_GET_PREFIXES = {
            "/api/accommodation",
            "/api/carousel/active",
            "/api/comment/page",
            "/api/comment/scenic/",
            "/api/feed.xml",
            "/api/rss.xml",
            "/api/scenic",
            "/api/seo",
            "/api/search",
            "/api/site/access/public",
            "/api/site/assets/public",
            "/api/site/footer/public",
            "/api/site/page-content/public",
            "/api/sitemap.html",
            "/api/sitemap.txt",
            "/api/sitemap.xml",
            "/api/tour",
            "/api/tour-detail",
            "/api/tour-hotel",
            "/api/tour-order-pay/methods"
    };
    
    // 将用户ID存储到请求属性中的key
    public static final String USER_ID_ATTRIBUTE = "userId";
    
    // Redis中token的key前缀
    private static final String USER_TOKEN_KEY_PREFIX = "user:token:";
    
    @Resource
    private UserService userService;
    
    @Resource
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (isPublicGet(request)) {
            return true;
        }
        String token = JwtTokenUtils.resolveToken(request);
        if (StringUtils.isBlank(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401状态码
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"code\":\"401\",\"msg\":\"请先登录\"}"); // 返回JSON错误信息
            return false;
        }

        // 首先检查Redis中token是否还存在，如果不存在说明已过期
        String redisUserId = (String) redisUtil.get(USER_TOKEN_KEY_PREFIX + token);
        if (StringUtils.isBlank(redisUserId)) {
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
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    private boolean isPublicGet(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (isPublicGuideGet(path)) {
            return true;
        }
        for (String prefix : PUBLIC_GET_PREFIXES) {
            if (matchesPathPrefix(path, prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicGuideGet(String path) {
        return path.equals("/api/guide/page")
                || path.equals("/api/guide/hot")
                || path.equals("/api/guide/suggestions")
                || path.matches("^/api/guide/\\d+$");
    }

    private boolean matchesPathPrefix(String path, String prefix) {
        if (path.equals(prefix)) {
            return true;
        }
        String boundaryPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return path.startsWith(boundaryPrefix);
    }

}
