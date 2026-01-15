package com.mayday.auth.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.mayday.auth.model.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务
 * <p>
 * 负责 JWT Token 的创建、解析、刷新和缓存管理。
 * 采用 JWT + Redis 的双重验证模式。
 * </p>
 *
 * <h3>Token 类型:</h3>
 * <ul>
 *   <li><b>临时 Token (tempToken)</b>: 多部门用户登录时使用，仅包含 userId，有效期短 (5分钟)</li>
 *   <li><b>最终 Token</b>: 包含 userId + currentDeptId，正常有效期</li>
 * </ul>
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Slf4j
@Service
public class TokenService {

    protected static final String TOKEN_HEADER = "Authorization";
    protected static final String TOKEN_PREFIX = "Bearer ";
    private static final String LOGIN_TOKEN_KEY = "login_tokens:";
    private static final String TEMP_TOKEN_KEY = "temp_tokens:";

    @Value("${mayday.auth.jwt.secret:mayday-security-jwt-secret-key-must-be-at-least-256-bits}")
    private String secret;

    @Value("${mayday.auth.jwt.expire-minutes:30}")
    private int expireMinutes;

    @Value("${mayday.auth.jwt.refresh-threshold-minutes:10}")
    private int refreshThresholdMinutes;

    /**
     * 临时 Token 有效期 (分钟)
     */
    @Value("${mayday.auth.jwt.temp-expire-minutes:5}")
    private int tempExpireMinutes;

    private final RedisTemplate<String, Object> redisTemplate;

    public TokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 创建临时 Token (用于多部门用户登录选择部门)
     * <p>
     * 临时 Token 仅包含 userId，有效期较短 (默认5分钟)。
     * 用户选择部门后，凭此 Token 换取最终 Token。
     * </p>
     *
     * @param userId 用户ID
     * @return 临时 Token
     */
    public String createTempToken(Long userId) {
        String uuid = UUID.fastUUID().toString(true);
        
        // 存入 Redis
        String redisKey = getTempTokenKey(uuid);
        redisTemplate.opsForValue().set(redisKey, userId, tempExpireMinutes, TimeUnit.MINUTES);
        
        // 生成 JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("uuid", uuid);
        claims.put("isTemp", true);
        
        log.debug("创建临时 Token, userId: {}, uuid: {}", userId, uuid);
        return createJwtToken(claims);
    }

    /**
     * 验证临时 Token 并获取 userId
     *
     * @param tempToken 临时 Token
     * @return 用户ID，如果 Token 无效返回 null
     */
    public Long validateTempToken(String tempToken) {
        try {
            Claims claims = parseToken(tempToken);
            Boolean isTemp = (Boolean) claims.get("isTemp");
            if (isTemp == null || !isTemp) {
                log.warn("Token 不是临时 Token");
                return null;
            }
            
            String uuid = (String) claims.get("uuid");
            String redisKey = getTempTokenKey(uuid);
            Object cachedValue = redisTemplate.opsForValue().get(redisKey);
            
            if (cachedValue == null) {
                log.warn("临时 Token 已过期或不存在, uuid: {}", uuid);
                return null;
            }
            
            // 处理可能的 Integer/Long 类型转换
            Long userId;
            if (cachedValue instanceof Long) {
                userId = (Long) cachedValue;
            } else if (cachedValue instanceof Integer) {
                userId = ((Integer) cachedValue).longValue();
            } else if (cachedValue instanceof Number) {
                userId = ((Number) cachedValue).longValue();
            } else {
                log.warn("临时 Token 缓存值类型不正确: {}", cachedValue.getClass());
                return null;
            }
            
            log.debug("临时 Token 验证成功, userId: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("验证临时 Token 失败", e);
            return null;
        }
    }

    /**
     * 删除临时 Token (用户选择部门后删除)
     *
     * @param tempToken 临时 Token
     */
    public void deleteTempToken(String tempToken) {
        try {
            Claims claims = parseToken(tempToken);
            String uuid = (String) claims.get("uuid");
            String redisKey = getTempTokenKey(uuid);
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("删除临时 Token 失败", e);
        }
    }

    /**
     * 创建最终 Token
     *
     * @param loginUser 登录用户信息 (必须包含 currentDeptId)
     * @return JWT Token
     */
    public String createToken(LoginUser loginUser) {
        String uuid = UUID.fastUUID().toString(true);
        loginUser.setToken(uuid);
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireMinutes * 60 * 1000L);

        // 将用户信息存入 Redis
        String redisKey = getTokenKey(uuid);
        redisTemplate.opsForValue().set(redisKey, loginUser, expireMinutes, TimeUnit.MINUTES);

        // 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", loginUser.getUserId());
        claims.put("currentDeptId", loginUser.getCurrentDeptId());
        claims.put("uuid", uuid);

        log.debug("创建最终 Token, userId: {}, deptId: {}", loginUser.getUserId(), loginUser.getCurrentDeptId());
        return createJwtToken(claims);
    }

    /**
     * 从请求中获取登录用户
     *
     * @param request HTTP 请求
     * @return 登录用户信息，如果 Token 无效则返回 null
     */
    public LoginUser getLoginUser(HttpServletRequest request) {
        String token = getToken(request);
        if (StrUtil.isNotEmpty(token)) {
            try {
                Claims claims = parseToken(token);
                
                // 跳过临时 Token
                Boolean isTemp = (Boolean) claims.get("isTemp");
                if (isTemp != null && isTemp) {
                    return null;
                }
                
                String uuid = (String) claims.get("uuid");
                String redisKey = getTokenKey(uuid);
                return (LoginUser) redisTemplate.opsForValue().get(redisKey);
            } catch (Exception e) {
                log.error("解析 Token 失败", e);
            }
        }
        return null;
    }

    /**
     * 验证并刷新 Token
     */
    public void verifyToken(LoginUser loginUser) {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= refreshThresholdMinutes * 60 * 1000L) {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新 Token
     */
    public void refreshToken(LoginUser loginUser) {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireMinutes * 60 * 1000L);
        String redisKey = getTokenKey(loginUser.getToken());
        redisTemplate.opsForValue().set(redisKey, loginUser, expireMinutes, TimeUnit.MINUTES);
    }

    /**
     * 删除 Token
     */
    public void deleteToken(String token) {
        if (StrUtil.isNotEmpty(token)) {
            String redisKey = getTokenKey(token);
            redisTemplate.delete(redisKey);
        }
    }
    
    /**
     * 删除用户的所有 Token
     * <p>
     * 用于用户权限变更后强制重新登录。
     * 遍历所有 login_tokens 找到该用户的 Token 并删除。
     * </p>
     *
     * @param userId 用户ID
     */
    public void deleteTokensByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        
        // 扫描所有 login_tokens 开头的 key
        Set<String> keys = redisTemplate.keys(LOGIN_TOKEN_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        int deletedCount = 0;
        for (String key : keys) {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj instanceof LoginUser) {
                LoginUser loginUser = (LoginUser) obj;
                if (userId.equals(loginUser.getUserId())) {
                    redisTemplate.delete(key);
                    deletedCount++;
                }
            }
        }
        
        log.info("用户 {} 的 {} 个 Token 已删除（权限变更）", userId, deletedCount);
    }

    /**
     * 从请求中获取 Token
     * <p>
     * 优先从 Authorization 请求头获取，如果没有则从 query 参数 'token' 获取。
     * 后者用于支持 SSE (Server-Sent Events) 的 EventSource，因为它不支持自定义请求头。
     * </p>
     */
    private String getToken(HttpServletRequest request) {
        // 优先从 Authorization 请求头获取
        String token = request.getHeader(TOKEN_HEADER);
        if (StrUtil.isNotEmpty(token) && token.startsWith(TOKEN_PREFIX)) {
            token = token.substring(TOKEN_PREFIX.length());
            return token;
        }
        
        // 如果请求头没有，从 query 参数获取（用于 SSE EventSource）
        token = request.getParameter("token");
        if (StrUtil.isNotEmpty(token)) {
            return token;
        }
        
        return null;
    }

    private String createJwtToken(Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claims(claims)
                .signWith(key)
                .compact();
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String getTokenKey(String uuid) {
        return LOGIN_TOKEN_KEY + uuid;
    }

    private String getTempTokenKey(String uuid) {
        return TEMP_TOKEN_KEY + uuid;
    }
}
