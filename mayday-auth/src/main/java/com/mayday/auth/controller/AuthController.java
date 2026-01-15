package com.mayday.auth.controller;

import com.mayday.auth.common.R;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.model.dto.LoginRequest;
import com.mayday.auth.model.dto.SelectDeptRequest;
import com.mayday.auth.model.dto.SwitchDeptRequest;
import com.mayday.auth.model.vo.LoginVo;
import com.mayday.auth.service.SysLoginService;
import com.mayday.auth.service.TokenService;
import com.mayday.auth.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * <p>
 * 提供登录、选择部门、切换部门、登出等接口。
 * </p>
 *
 * <h3>多部门登录流程:</h3>
 * <ol>
 *   <li>调用 {@code POST /login}，如果返回 {@code needSelectDept=true}，进入第2步</li>
 *   <li>调用 {@code POST /selectDept}，传入 {@code tempToken} 和选择的 {@code deptId}</li>
 *   <li>获取最终 Token，后续请求携带此 Token</li>
 * </ol>
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final SysLoginService loginService;
    private final TokenService tokenService;

    /**
     * 用户登录 (第一步)
     * <p>
     * 验证用户名密码。根据用户所属部门数量，返回不同结果：
     * <ul>
     *   <li><b>单部门</b>: 返回 {@code token}，可直接使用</li>
     *   <li><b>多部门</b>: 返回 {@code tempToken} + {@code deptList}，需调用 selectDept</li>
     * </ul>
     * </p>
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public R<LoginVo> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginVo vo = loginService.login(request.getUsername(), request.getPassword());
            return R.ok(vo);
        } catch (Exception e) {
            log.error("登录失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 选择部门 (第二步 - 仅多部门用户需要)
     * <p>
     * 多部门用户登录后，凭临时 Token 选择部门，生成最终 Token。
     * </p>
     *
     * @param request 选择部门请求
     * @return 登录响应 (包含最终 Token)
     */
    @PostMapping("/selectDept")
    public R<LoginVo> selectDept(@Valid @RequestBody SelectDeptRequest request) {
        try {
            LoginVo vo = loginService.selectDept(request.getTempToken(), request.getDeptId());
            return R.ok(vo);
        } catch (Exception e) {
            log.error("选择部门失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 切换部门 (已登录用户)
     * <p>
     * 验证用户是否属于目标部门，然后重新生成 Token，更新角色和权限。
     * </p>
     *
     * @param request 切换部门请求
     * @return 登录响应 (包含新 Token)
     */
    @PostMapping("/switchDept")
    public R<LoginVo> switchDept(@Valid @RequestBody SwitchDeptRequest request) {
        try {
            LoginUser currentUser = SecurityUtils.getLoginUser();
            LoginVo vo = loginService.switchDept(currentUser, request.getDeptId());
            return R.ok(vo);
        } catch (Exception e) {
            log.error("切换部门失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null) {
                tokenService.deleteToken(loginUser.getToken());
                log.info("用户 {} 登出成功", loginUser.getUsername());
            }
        } catch (Exception e) {
            log.warn("登出过程发生异常", e);
        }
        return R.ok();
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/getInfo")
    public R<LoginVo> getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        
        LoginVo vo = new LoginVo();
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setAvatar(loginUser.getAvatar());
        vo.setCurrentDeptId(loginUser.getCurrentDeptId());
        vo.setPermissions(new java.util.ArrayList<>(loginUser.getPermissions()));
        
        return R.ok(vo);
    }
}
