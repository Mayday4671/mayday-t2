package com.mayday.common.manager;

import com.mayday.common.util.SpringContextUtils;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;


/**
 * @Description: 日志动态配置
 * @Author: lc
 * @Date: 2025/7/22 20:30
 * @Version: 1.0
 */
@Component
public class DynamicLogLevelManager
{
    /**
     * 设置日志级别
     *
     * @param packageName
     * @param level
     */
    public void setLogLevel(String packageName, LogLevel level)
    {
        LoggingSystem loggingSystem = SpringContextUtils.getBean(LoggingSystem.class);
        loggingSystem.setLogLevel(packageName, level);
    }

    /**
     * 清空日志级别
     *
     * @param packageName
     */
    public void clearLogLevel(String packageName)
    {
        LoggingSystem loggingSystem = SpringContextUtils.getBean(LoggingSystem.class);
        loggingSystem.setLogLevel(packageName, null);
    }
}
