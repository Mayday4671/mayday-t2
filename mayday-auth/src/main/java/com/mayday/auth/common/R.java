package com.mayday.auth.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果封装
 * <p>
 * 提供标准化的 API 响应格式，包含状态码、消息和数据。
 * </p>
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 成功
 * return R.ok(data);
 * 
 * // 失败
 * return R.fail("操作失败");
 * 
 * // 自定义状态码
 * return R.fail(401, "未授权");
 * }</pre>
 *
 * @param <T> 数据类型
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码
     */
    public static final int SUCCESS = 200;

    /**
     * 失败状态码
     */
    public static final int FAIL = 500;

    /**
     * 状态码
     */
    private int code;

    /**
     * 消息
     */
    private String msg;

    /**
     * 数据
     */
    private T data;

    /**
     * 私有构造函数
     */
    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 返回成功结果 (无数据)
     */
    public static <T> R<T> ok() {
        return new R<>(SUCCESS, "操作成功", null);
    }

    /**
     * 返回成功结果 (带数据)
     */
    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS, "操作成功", data);
    }

    /**
     * 返回成功结果 (自定义消息)
     */
    public static <T> R<T> ok(T data, String msg) {
        return new R<>(SUCCESS, msg, data);
    }

    /**
     * 返回失败结果
     */
    public static <T> R<T> fail() {
        return new R<>(FAIL, "操作失败", null);
    }

    /**
     * 返回失败结果 (自定义消息)
     */
    public static <T> R<T> fail(String msg) {
        return new R<>(FAIL, msg, null);
    }

    /**
     * 返回失败结果 (自定义状态码和消息)
     */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return SUCCESS == this.code;
    }
}
