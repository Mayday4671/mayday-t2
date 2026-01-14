package com.mayday.common.constant;

/**
 * 缓存相关常量。
 *
 * 这里只定义当前项目实际用到的常量，方便 Redis key 统一管理。
 */
public final class CacheConstants
{
    private CacheConstants()
    {
    }

    /**
     * 字典缓存前缀
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 参数配置缓存前缀
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";
}


