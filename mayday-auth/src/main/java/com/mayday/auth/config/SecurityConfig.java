package com.mayday.auth.config;

import com.mayday.auth.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 配置类
 * <p>
 * 配置无状态 (Stateless) 的 JWT 认证模式，禁用 Session，
 * 并注册 {@link JwtAuthenticationFilter} 用于 Token 解析和认证。
 * </p>
 *
 * <h3>核心配置:</h3>
 * <ul>
 *   <li><b>无状态模式</b>: {@code SessionCreationPolicy.STATELESS}</li>
 *   <li><b>禁用 CSRF</b>: 因为使用 JWT，不需要 CSRF 保护</li>
 *   <li><b>禁用表单登录和 HTTP Basic</b>: 使用自定义的登录接口</li>
 *   <li><b>方法级安全</b>: 启用 {@code @PreAuthorize}, {@code @PostAuthorize} 等注解</li>
 * </ul>
 *
 * <h3>白名单配置:</h3>
 * <p>
 * 登录接口、验证码、静态资源等匿名访问路径应在 {@code requestMatchers(...).permitAll()} 中配置。
 * </p>
 *
 * @author MayDay Auth Generator
 * @see JwtAuthenticationFilter
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 匿名访问白名单 (可从配置文件读取)
     */
    private static final String[] ANONYMOUS_URLS = {
            "/login",
            "/selectDept",
            "/register",
            "/captcha/**",
            "/debug/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/webjars/**",
            "/crawler-images/**",  // 爬虫图片静态资源
            "/upload/**"           // 上传文件静态资源
    };

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 配置 SecurityFilterChain
     * <p>
     * Spring Security 6 推荐使用 SecurityFilterChain Bean 替代继承 WebSecurityConfigurerAdapter。
     * </p>
     *
     * @param http HttpSecurity 配置对象
     * @return 配置好的 SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF (使用 JWT，不需要 CSRF)
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用 HTTP Basic 认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 禁用表单登录
                .formLogin(AbstractHttpConfigurer::disable)
                // 基于 Token，不创建 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 白名单放行
                        .requestMatchers(ANONYMOUS_URLS).permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                // 添加 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器
     * <p>
     * 使用 BCrypt 算法进行密码加密，强度为 10 (默认值)。
     * </p>
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager Bean
     * <p>
     * 用于手动触发认证，如登录接口中调用 {@code authenticationManager.authenticate(...)}。
     * </p>
     *
     * @param authenticationConfiguration 认证配置
     * @return AuthenticationManager 实例
     * @throws Exception 获取异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
