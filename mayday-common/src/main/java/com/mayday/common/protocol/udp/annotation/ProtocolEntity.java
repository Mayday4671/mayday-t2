package com.mayday.common.protocol.udp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 协议实体类注解
 * 用于标记协议实体类，支持前端展示和解析
 * 
 * @author mayday
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtocolEntity {
    
    /**
     * 实体名称（用于前端展示）
     */
    String name();
    
    /**
     * 实体描述
     */
    String description() default "";
    
    /**
     * 协议版本
     */
    int version() default 1;
    
    /**
     * 总长度（字节数）
     */
    int totalLength() default -1;
}



