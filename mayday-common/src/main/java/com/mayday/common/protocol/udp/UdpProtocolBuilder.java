package com.mayday.common.protocol.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

/**
 * UDP协议构建器
 * 负责将UdpProtocolMessage对象编码为字节流
 * 
 * @author mayday
 */
@Slf4j
public class UdpProtocolBuilder {
    
    /**
     * 构建协议消息的字节数组
     * 
     * @param message 协议消息对象
     * @return 字节数组
     */
    public static byte[] build(UdpProtocolMessage message) {
        if (message == null || message.getHeader() == null) {
            throw new IllegalArgumentException("协议消息或协议头不能为空");
        }
        
        UdpProtocolHeader header = message.getHeader();
        byte[] payload = message.getPayload();
        
        // 验证数据长度
        int dataLength = payload != null ? payload.length : 0;
        if (dataLength > UdpProtocolParser.MAX_DATA_LENGTH) {
            throw new IllegalArgumentException("数据长度超出限制: " + dataLength);
        }
        
        // 设置数据长度
        header.setDataLength(dataLength);
        
        // 如果没有设置时间戳，使用当前时间
        if (header.getTimestamp() == 0) {
            header.setTimestamp(System.currentTimeMillis() / 1000);
        }
        
        // 分配缓冲区（协议头10字节 + 数据长度）
        ByteBuf buffer = Unpooled.buffer(UdpProtocolParser.HEADER_LENGTH + dataLength);
        
        try {
            // 构建第一个字节（版本+帧类型+保留）
            byte byte0 = 0;
            byte0 |= (header.getVersion() & 0x03) << 6;  // 版本占高2位
            byte0 |= (header.getFrameType().getCode() & 0x0F) << 2;  // 帧类型占中间4位
            byte0 |= (header.getReserved() & 0x03);  // 保留占低2位
            buffer.writeByte(byte0);
            
            // 写入标志位
            buffer.writeByte(header.getFlags());
            
            // 写入序列号（2字节，大端序）
            buffer.writeShort(header.getSequence());
            
            // 写入数据长度（2字节，大端序）
            buffer.writeShort(dataLength);
            
            // 写入时间戳（4字节，大端序）
            buffer.writeInt((int) header.getTimestamp());
            
            // 写入净荷数据
            if (payload != null && payload.length > 0) {
                buffer.writeBytes(payload);
            }
            
            // 转换为字节数组
            byte[] result = new byte[buffer.readableBytes()];
            buffer.readBytes(result);
            
            log.debug("协议构建成功: version={}, frameType={}, sequence={}, dataLength={}", 
                    header.getVersion(), header.getFrameType(), header.getSequence(), dataLength);
            
            return result;
        } finally {
            buffer.release();
        }
    }
    
    /**
     * 构建心跳包
     * 
     * @param sequence 序列号
     * @return 协议消息
     */
    public static UdpProtocolMessage buildHeartbeat(int sequence) {
        UdpProtocolHeader header = new UdpProtocolHeader();
        header.setVersion(1);
        header.setFrameType(UdpProtocolHeader.FrameType.HEARTBEAT);
        header.setSequence(sequence);
        header.setFlags(UdpProtocolHeader.Flags.setNeedResponse(header.getFlags(), false));
        
        return new UdpProtocolMessage(header, null);
    }
    
    /**
     * 构建数据包
     * 
     * @param sequence 序列号
     * @param payload 数据载荷
     * @param needResponse 是否需要响应
     * @return 协议消息
     */
    public static UdpProtocolMessage buildData(int sequence, byte[] payload, boolean needResponse) {
        UdpProtocolHeader header = new UdpProtocolHeader();
        header.setVersion(1);
        header.setFrameType(UdpProtocolHeader.FrameType.DATA);
        header.setSequence(sequence);
        header.setFlags(UdpProtocolHeader.Flags.setNeedResponse(header.getFlags(), needResponse));
        
        return new UdpProtocolMessage(header, payload);
    }
    
    /**
     * 构建数据包（字符串）
     * 
     * @param sequence 序列号
     * @param payload 数据载荷（字符串）
     * @param needResponse 是否需要响应
     * @return 协议消息
     */
    public static UdpProtocolMessage buildData(int sequence, String payload, boolean needResponse) {
        return buildData(sequence, payload != null ? payload.getBytes() : null, needResponse);
    }
    
    /**
     * 构建响应包
     * 
     * @param sequence 序列号（对应请求的序列号）
     * @param payload 响应数据
     * @return 协议消息
     */
    public static UdpProtocolMessage buildResponse(int sequence, byte[] payload) {
        UdpProtocolHeader header = new UdpProtocolHeader();
        header.setVersion(1);
        header.setFrameType(UdpProtocolHeader.FrameType.RESPONSE);
        header.setSequence(sequence);
        header.setFlags(UdpProtocolHeader.Flags.setNeedResponse(header.getFlags(), false));
        
        return new UdpProtocolMessage(header, payload);
    }
    
    /**
     * 构建响应包（字符串）
     * 
     * @param sequence 序列号
     * @param payload 响应数据（字符串）
     * @return 协议消息
     */
    public static UdpProtocolMessage buildResponse(int sequence, String payload) {
        return buildResponse(sequence, payload != null ? payload.getBytes() : null);
    }
    
    /**
     * 构建控制包
     * 
     * @param sequence 序列号
     * @param payload 控制数据
     * @return 协议消息
     */
    public static UdpProtocolMessage buildControl(int sequence, byte[] payload) {
        UdpProtocolHeader header = new UdpProtocolHeader();
        header.setVersion(1);
        header.setFrameType(UdpProtocolHeader.FrameType.CONTROL);
        header.setSequence(sequence);
        header.setFlags(UdpProtocolHeader.Flags.setNeedResponse(header.getFlags(), true));
        
        return new UdpProtocolMessage(header, payload);
    }
}

