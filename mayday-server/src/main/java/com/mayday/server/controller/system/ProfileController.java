package com.mayday.server.controller.system;

import com.mayday.auth.entity.SysUser;
import com.mayday.auth.mapper.SysUserMapper;
import com.mayday.auth.model.LoginUser;
import com.mayday.common.enums.ErrorCode;
import com.mayday.common.exception.BusinessException;
import com.mayday.auth.common.R;
import com.mayday.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.HttpURLConnection;

/**
 * 个人中心控制器
 * 
 * 提供当前登录用户的个人信息查询和修改功能。
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/profile")
@Tag(name = "个人中心")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${mayday.upload.path}")
    private String uploadPath;

    /**
     * 获取当前用户信息
     */
    @GetMapping
    @Operation(summary = "获取当前用户信息")
    public R<Map<String, Object>> getProfile() {
        LoginUser loginUser = getCurrentLoginUser();
        SysUser user = userMapper.selectOneById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("avatar", user.getAvatar());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("createTime", user.getCreateTime());

        return R.ok(profile);
    }

    /**
     * 更新个人信息（昵称、邮箱、手机号）
     */
    @PutMapping
    @Operation(summary = "更新个人信息")
    public R<Void> updateProfile(@RequestBody ProfileUpdateReq req) {
        LoginUser loginUser = getCurrentLoginUser();
        SysUser user = userMapper.selectOneById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 更新字段
        if (StringUtils.isNotEmpty(req.getNickname())) {
            user.setNickname(req.getNickname());
        }
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }
        user.setUpdateTime(new Date());
        
        userMapper.update(user);
        return R.ok();
    }

    /**
     * 更新头像
     */
    @PutMapping("/avatar")
    @Operation(summary = "更新头像")
    public R<Void> updateAvatar(@RequestBody AvatarUpdateReq req) {
        if (StringUtils.isEmpty(req.getAvatar())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像URL不能为空");
        }

        LoginUser loginUser = getCurrentLoginUser();
        SysUser user = userMapper.selectOneById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        user.setAvatar(req.getAvatar());
        user.setUpdateTime(new Date());
        userMapper.update(user);

        return R.ok();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public R<Void> updatePassword(@RequestBody PasswordUpdateReq req) {
        if (StringUtils.isEmpty(req.getOldPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前密码不能为空");
        }
        if (StringUtils.isEmpty(req.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能为空");
        }
        if (req.getNewPassword().length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能少于6位");
        }

        LoginUser loginUser = getCurrentLoginUser();
        SysUser user = userMapper.selectOneById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前密码错误");
        }

        // 设置新密码
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdateTime(new Date());
        userMapper.update(user);

        return R.ok();
    }

    /**
     * 上传头像 (直接上传文件)
     */
    @PostMapping("/avatar/upload")
    @Operation(summary = "上传头像")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        try {
            // 确保目录存在
            File uploadDir = new File(uploadPath + "avatar/");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            File dest = new File(uploadDir, fileName);
            file.transferTo(dest);
            
            // 返回访问URL (假设配置了 /profile/avatar/** 映射到本地目录)
            // 注意：这里需要前端拼接 host，或者后端返回完整 URL
            // 为了简单，我们配置 Tomcat/WebMvc 将 /upload/** 映射到 uploadPath
            String avatarUrl = "/upload/avatar/" + fileName;
            
            // 更新用户头像
            LoginUser loginUser = getCurrentLoginUser();
            SysUser user = userMapper.selectOneById(loginUser.getUserId());
            if (user != null) {
                user.setAvatar(avatarUrl);
                user.setUpdateTime(new Date());
                userMapper.update(user);
            }
            
            return R.ok(avatarUrl);
            
        } catch (Exception e) {
            log.error("上传头像失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传头像失败: " + e.getMessage());
        }
    }

    /**
     * 上传头像 (通过网络URL)
     */
    @PostMapping("/avatar/url")
    @Operation(summary = "上传头像(网络URL)")
    public R<String> uploadAvatarByUrl(@RequestBody AvatarUpdateReq req) {
        String urlStr = req.getAvatar();
        if (StringUtils.isEmpty(urlStr)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL不能为空");
        }

        try {
            // 确保目录存在
            File uploadDir = new File(uploadPath + "avatar/");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成文件名 (尝试从URL获取后缀，默认为.jpg)
            String extension = ".jpg";
            if (urlStr.contains(".")) {
                 String tempExt = urlStr.substring(urlStr.lastIndexOf("."));
                 // 处理像 .jpeg@f_auto 这样的后缀
                 if (tempExt.contains("@") || tempExt.contains("?")) {
                     tempExt = tempExt.split("[@?]")[0];
                 }
                 if (tempExt.length() <= 5) { // 简单校验后缀长度
                     extension = tempExt;
                 }
            }
            String fileName = UUID.randomUUID().toString() + extension;
            File dest = new File(uploadDir, fileName);

            // 下载文件
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0"); // 伪装 User-Agent 防止部分防爬
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // 返回访问URL
            String avatarUrl = "/upload/avatar/" + fileName;

            // 更新用户头像
            LoginUser loginUser = getCurrentLoginUser();
            SysUser user = userMapper.selectOneById(loginUser.getUserId());
            if (user != null) {
                user.setAvatar(avatarUrl);
                user.setUpdateTime(new Date());
                userMapper.update(user);
            }

            return R.ok(avatarUrl);

        } catch (Exception e) {
            log.error("转存网络图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片下载失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户
     */
    private LoginUser getCurrentLoginUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
    }

    // ==================== 请求DTO ====================

    @Data
    public static class ProfileUpdateReq {
        private String nickname;
        private String email;
        private String phone;
    }

    @Data
    public static class AvatarUpdateReq {
        private String avatar;
    }

    @Data
    public static class PasswordUpdateReq {
        private String oldPassword;
        private String newPassword;
    }
}
