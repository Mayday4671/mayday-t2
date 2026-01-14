package com.mayday.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 权限模块演示应用启动类
 * <p>
 * 用于测试和演示 mayday-auth 权限模块的功能。
 * 通过 {@code scanBasePackages} 扫描核心权限模块的组件。
 * </p>
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.mayday.auth", "com.mayday.server","com.mayday.crawler"})
@MapperScan("com.mayday.**.mapper")
public class MaydayServerApplication
{

    public static void main(String[] args) {
        SpringApplication.run(MaydayServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("  MayDay Auth Demo Started Successfully!");
        System.out.println("========================================");
    }
}
