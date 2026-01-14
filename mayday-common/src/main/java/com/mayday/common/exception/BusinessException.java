package com.mayday.common.exception;

import com.mayday.common.enums.ErrorCode;
import lombok.Getter;


/**
 * @author: lc
 * @date: 2025/5/11 20:30
 */
@Getter
public class BusinessException extends RuntimeException
{

    /**
     * 错误码
     */
    private final int code;

    private BusinessException(int code)
    {
        this.code = code;
    }

    /**
     * 功能描述
     *
     * @param msg
     *
     * @author: lc
     * @date: 2025/5/11
     * @param: * @param code
     * @return:
     */
    public BusinessException(int code, String msg)
    {
        super(msg);
        this.code = code;
    }

    public BusinessException(ErrorCode code)
    {
        super(code.getMsg());
        this.code = code.getCode();
    }

    public BusinessException(ErrorCode code, String msg)
    {
        super(msg);
        this.code = code.getCode();
    }
}
