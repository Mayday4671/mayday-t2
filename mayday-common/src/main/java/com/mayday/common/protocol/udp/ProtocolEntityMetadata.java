package com.mayday.common.protocol.udp;

import com.mayday.common.protocol.udp.annotation.ProtocolEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 协议实体元数据
 * 用于前端展示和解析
 * 
 * @author mayday
 */
@Data
public class ProtocolEntityMetadata {
    
    /**
     * 实体名称
     */
    private String name;
    
    /**
     * 实体描述
     */
    private String description;
    
    /**
     * 协议版本
     */
    private int version;
    
    /**
     * 总长度（字节数）
     */
    private int totalLength;
    
    /**
     * 字段列表
     */
    private List<ProtocolFieldMetadata> fields;
    
    /**
     * 原始16进制数据
     */
    private String hexData;
    
    public ProtocolEntityMetadata() {
        this.fields = new ArrayList<>();
    }
    
    public ProtocolEntityMetadata(ProtocolEntity annotation) {
        this.name = annotation.name();
        this.description = annotation.description();
        this.version = annotation.version();
        this.totalLength = annotation.totalLength();
        this.fields = new ArrayList<>();
    }
}



