package com.mayday.common.protocol.udp;

import com.mayday.common.protocol.udp.annotation.ProtocolEntity;
import com.mayday.common.protocol.udp.annotation.ProtocolField;
import lombok.Data;

/**
 * UDP协议公共头
 * 
 * 协议头结构（共10字节）：
 * +---------------+---------------+---------------+---------------+---------------+
 * | 版本(2) | 帧类型(4) | 保留(2) | 标志位(8) | 序列号(16) | 数据长度(16) |
 * +---------------+---------------+---------------+---------------+---------------+
 * | 时间戳(32)                                                    |
 * +---------------------------------------------------------------+
 * 
 * 标志位（1字节）拆分：
 * - bit 0: 是否需要响应（1=需要，0=不需要）
 * - bit 1: 是否加密（1=加密，0=明文）
 * - bit 2: 是否压缩（1=压缩，0=未压缩）
 * - bit 3: 是否分片（1=分片，0=完整）
 * - bit 4-7: 保留
 * 
 * @author mayday
 */
@Data
@ProtocolEntity(name = "UDP协议头", description = "UDP协议公共头，共10字节", version = 1, totalLength = 10)
public class UdpProtocolHeader {
    
    /**
     * 协议版本（2位，0-3）
     * 当前版本：1
     */
    @ProtocolField(name = "协议版本", description = "协议版本", byteOffset = 0, byteLength = 2, type = ProtocolField.FieldType.INT, format = "十进制")
    private int version = 1;
    
    /**
     * 帧类型（4位，0-15）
     * 0x0: 心跳包
     * 0x1: 数据包
     * 0x2: 控制包
     * 0x3: 响应包
     * 0x4-0xF: 保留
     */
    @ProtocolField(name = "帧类型", description = "帧类型", byteOffset = 0, byteLength = 4, type = ProtocolField.FieldType.ENUM, format = "枚举")
    private FrameType frameType;
    
    /**
     * 保留字段（2位）
     */
    @ProtocolField(name = "保留字段", description = "保留字段", byteOffset = 0, byteLength = 2, type = ProtocolField.FieldType.INT, required = false, format = "二进制")
    private int reserved = 0;
    
    /**
     * 标志位（1字节，8位）
     */
    @ProtocolField(name = "标志位", description = "标志位", byteOffset = 1, byteLength = 1, type = ProtocolField.FieldType.BYTE, format = "16进制")
    private byte flags = 0;
    
    /**
     * 序列号（2字节，0-65535）
     * 用于请求-响应匹配和去重
     */
    @ProtocolField(name = "序列号", description = "序列号", byteOffset = 2, byteLength = 2, type = ProtocolField.FieldType.SHORT, format = "十进制")
    private int sequence;
    
    /**
     * 数据长度（2字节，0-65535）
     * 净荷数据的长度（字节数）
     */
    @ProtocolField(name = "数据长度", description = "数据长度", byteOffset = 4, byteLength = 2, type = ProtocolField.FieldType.SHORT, format = "十进制")
    private int dataLength;
    
    /**
     * 时间戳（4字节）
     * Unix时间戳（秒）
     */
    @ProtocolField(name = "时间戳", description = "时间戳", byteOffset = 6, byteLength = 4, type = ProtocolField.FieldType.INT, format = "Unix时间戳")
    private long timestamp;
    
    /**
     * 帧类型枚举
     */
    public enum FrameType {
        HEARTBEAT(0x0, "心跳包"),
        DATA(0x1, "数据包"),
        CONTROL(0x2, "控制包"),
        RESPONSE(0x3, "响应包");
        
        private final int code;
        private final String description;
        
        FrameType(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static FrameType fromCode(int code) {
            for (FrameType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的帧类型: " + code);
        }
    }
    
    /**
     * 标志位操作类
     */
    public static class Flags {
        private static final byte NEED_RESPONSE = 0x01;  // bit 0
        private static final byte ENCRYPTED = 0x02;      // bit 1
        private static final byte COMPRESSED = 0x04;      // bit 2
        private static final byte FRAGMENTED = 0x08;      // bit 3
        
        /**
         * 设置是否需要响应
         */
        public static byte setNeedResponse(byte flags, boolean need) {
            return need ? (byte)(flags | NEED_RESPONSE) : (byte)(flags & ~NEED_RESPONSE);
        }
        
        /**
         * 检查是否需要响应
         */
        public static boolean isNeedResponse(byte flags) {
            return (flags & NEED_RESPONSE) != 0;
        }
        
        /**
         * 设置是否加密
         */
        public static byte setEncrypted(byte flags, boolean encrypted) {
            return encrypted ? (byte)(flags | ENCRYPTED) : (byte)(flags & ~ENCRYPTED);
        }
        
        /**
         * 检查是否加密
         */
        public static boolean isEncrypted(byte flags) {
            return (flags & ENCRYPTED) != 0;
        }
        
        /**
         * 设置是否压缩
         */
        public static byte setCompressed(byte flags, boolean compressed) {
            return compressed ? (byte)(flags | COMPRESSED) : (byte)(flags & ~COMPRESSED);
        }
        
        /**
         * 检查是否压缩
         */
        public static boolean isCompressed(byte flags) {
            return (flags & COMPRESSED) != 0;
        }
        
        /**
         * 设置是否分片
         */
        public static byte setFragmented(byte flags, boolean fragmented) {
            return fragmented ? (byte)(flags | FRAGMENTED) : (byte)(flags & ~FRAGMENTED);
        }
        
        /**
         * 检查是否分片
         */
        public static boolean isFragmented(byte flags) {
            return (flags & FRAGMENTED) != 0;
        }
    }
}

