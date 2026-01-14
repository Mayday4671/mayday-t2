package com.mayday.common.exception;

import com.mayday.common.enums.ErrorCode;


/**
 * @Description: 异常处理工具类
 * @Author: lc
 * @Date: 2025/5/11 20:37
 * @Version: 1.0
 */
public class ThrowUtil
{

    /**
     * 条件成立则抛出异常和异常信息
     *
     * @param condition
     *     条件
     * @param errorCode
     *     状态码
     * @param msg
     *     错误信息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String msg)
    {
        throwIf(condition, new BusinessException(errorCode, msg));
    }

    /**
     * 条件成立抛出异常
     *
     * @param condition
     *     条件
     * @param runtimeException
     *     异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException)
    {
        if (condition)
        {
            throw runtimeException;
        }
    }

    /**
     * 条件成立，抛出自定义异常
     *
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition, ErrorCode errorCode)
    {
        if (condition)
        {
            throw new BusinessException(errorCode);
        }
    }
}
