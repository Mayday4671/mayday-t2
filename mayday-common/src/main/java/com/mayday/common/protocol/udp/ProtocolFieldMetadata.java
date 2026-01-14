package com.mayday.common.protocol.udp;

import com.mayday.common.protocol.udp.annotation.ProtocolField;
import lombok.Data;

/**
 * 协议字段元数据
 * 用于前端展示和解析
 * 
 * @author mayday
 */
@Data
public class ProtocolFieldMetadata {
    
    /**
     * 字段名称
     */
    private String name;
    
    /**
     * 字段描述
     */
    private String description;
    
    /**
     * 字段在协议中的位置（字节偏移）
     */
    private int byteOffset;
    
    /**
     * 字段长度（字节数）
     */
    private int byteLength;
    
    /**
     * 字段类型
     */
    private ProtocolField.FieldType type;
    
    /**
     * 是否必填
     */
    private boolean required;
    
    /**
     * 字段格式
     */
    private String format;
    
    /**
     * 字段值（用于前端展示）
     */
    private Object value;
    
    /**
     * 字段的16进制表示（用于前端展示）
     */
    private String hexValue;
    
    /**
     * 字段的二进制表示（用于前端展示）
     */
    private String binaryValue;
    
    /**
     * 字段的字符串表示（用于前端展示）
     */
    private String stringValue;
    
    public ProtocolFieldMetadata() {
    }
    
    public ProtocolFieldMetadata(ProtocolField annotation, Object value) {
        this.name = annotation.name();
        this.description = annotation.description();
        this.byteOffset = annotation.byteOffset();
        this.byteLength = annotation.byteLength();
        this.type = annotation.type();
        this.required = annotation.required();
        this.format = annotation.format();
        this.value = value;
    }
}



