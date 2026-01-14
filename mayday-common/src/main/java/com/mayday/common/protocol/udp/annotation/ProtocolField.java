package com.mayday.common.protocol.udp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 协议字段注解
 * 用于标记协议实体类中的字段，支持前端展示和解析
 * 
 * @author mayday
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtocolField {
    
    /**
     * 字段名称（用于前端展示）
     */
    String name();
    
    /**
     * 字段描述
     */
    String description() default "";
    
    /**
     * 字段在协议中的位置（字节偏移）
     */
    int byteOffset() default -1;
    
    /**
     * 字段长度（字节数）
     */
    int byteLength() default -1;
    
    /**
     * 字段类型（用于前端展示）
     */
    FieldType type() default FieldType.AUTO;
    
    /**
     * 是否必填
     */
    boolean required() default true;
    
    /**
     * 字段格式（如：16进制、10进制、二进制等）
     */
    String format() default "";
    
    /**
     * 字段类型枚举
     */
    enum FieldType {
        AUTO,           // 自动推断
        BYTE,           // 字节
        SHORT,          // 短整型（2字节）
        INT,            // 整型（4字节）
        LONG,           // 长整型（8字节）
        STRING,         // 字符串
        BYTES,          // 字节数组
        ENUM,           // 枚举
        BOOLEAN,        // 布尔值
        HEX_STRING,     // 16进制字符串
        BINARY_STRING   // 二进制字符串
    }
}



