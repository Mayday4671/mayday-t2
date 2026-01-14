package com.mayday.auth.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Flex 配置类
 * <p>
 * 配置 MyBatis Flex 的全局行为，包括审计日志、逻辑删除等。
 * </p>
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 开启审计功能 (开发环境打印 SQL)
        AuditManager.setAuditEnable(true);
        
        // 设置 SQL 审计收集器 (打印执行的 SQL)
        AuditManager.setMessageCollector(auditMessage -> 
            log.debug("SQL: {} | 耗时: {}ms", auditMessage.getFullSql(), auditMessage.getElapsedTime())
        );
    }
}
