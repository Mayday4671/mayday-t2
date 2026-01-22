package com.mayday.auth.filter;

import com.mayday.auth.model.LoginUser;
import com.mayday.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * <p>
 * 继承 {@link OncePerRequestFilter}，确保每个请求只执行一次过滤。
 * 负责从请求头中解析 JWT Token，验证其有效性，并将用户信息设置到 Spring Security 上下文中。
 * </p>
 *
 * <h3>工作流程:</h3>
 * <ol>
 *   <li>从请求头 {@code Authorization} 中提取 Bearer Token</li>
 *   <li>通过 {@link TokenService} 解析 Token 并从 Redis 获取用户信息</li>
 *   <li>如果用户信息有效，检查是否需要自动刷新 Token</li>
 *   <li>创建 {@link UsernamePasswordAuthenticationToken} 并设置到 {@link SecurityContextHolder}</li>
 *   <li>继续执行过滤器链</li>
 * </ol>
 *
 * <h3>注意事项:</h3>
 * <ul>
 *   <li>此过滤器应在 Spring Security 过滤器链中位于 {@code UsernamePasswordAuthenticationFilter} 之前</li>
 *   <li>对于匿名访问的接口，即使没有 Token 也会继续执行，由后续的授权逻辑处理</li>
 *   <li>Token 过期或无效时，SecurityContext 不会设置 Authentication，请求将被拦截</li>
 * </ul>
 *
 * @author MayDay Auth Generator
 * @see TokenService
 * @see OncePerRequestFilter
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 执行过滤逻辑
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");
        log.info("【Debug Filter】Processing request: {} {}, Authorization: {}", request.getMethod(), requestURI, 
                 authHeader != null ? (authHeader.length() > 20 ? authHeader.substring(0, 20) + "..." : authHeader) : "NULL");

        // 从请求中获取登录用户
        LoginUser loginUser = tokenService.getLoginUser(request);

        if (loginUser != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 验证 Token 有效期，必要时刷新
            tokenService.verifyToken(loginUser);

            // 创建 Authentication 对象并设置到 SecurityContext
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            log.debug("用户 {} 认证成功，当前部门: {}", loginUser.getUsername(), loginUser.getCurrentDeptId());
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}
