package com.mayday.common.protocol.udp;

import com.mayday.common.protocol.udp.annotation.ProtocolEntity;
import com.mayday.common.protocol.udp.annotation.ProtocolField;
import lombok.Data;

/**
 * UDP协议消息实体
 * 包含协议头和数据载荷
 * 
 * @author mayday
 */
@Data
@ProtocolEntity(name = "UDP协议消息", description = "UDP协议消息，包含公共头与净荷", version = 1)
public class UdpProtocolMessage {
    
    /**
     * 协议头
     */
    @ProtocolField(name = "协议头", description = "协议头", byteOffset = 0, byteLength = 10, type = ProtocolField.FieldType.AUTO)
    private UdpProtocolHeader header;
    
    /**
     * 数据载荷（净荷）
     */
    @ProtocolField(name = "数据载荷", description = "数据载荷", byteOffset = 10, type = ProtocolField.FieldType.BYTES, format = "16进制/字符串")
    private byte[] payload;
    
    /**
     * 净荷实体对象（根据帧类型解析）
     */
    private Object payloadEntity;
    
    /**
     * 原始字节数据（可选，用于调试）
     */
    @ProtocolField(name = "原始数据", description = "原始字节，用于调试", type = ProtocolField.FieldType.BYTES, required = false, format = "16进制")
    private byte[] rawData;
    
    public UdpProtocolMessage() {
        this.header = new UdpProtocolHeader();
    }
    
    public UdpProtocolMessage(UdpProtocolHeader header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }
    
    /**
     * 获取净荷的字符串形式
     */
    public String getPayloadAsString() {
        if (payload == null) {
            return null;
        }
        return new String(payload);
    }
    
    /**
     * 设置净荷（字符串）
     */
    public void setPayloadAsString(String payload) {
        if (payload != null) {
            this.payload = payload.getBytes();
            if (this.header != null) {
                this.header.setDataLength(this.payload.length);
            }
        }
    }
}

