package com.mayday.common.protocol.udp;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

/**
 * UDP协议解析器
 * 负责将字节流解析为UdpProtocolMessage对象
 * 
 * @author mayday
 */
@Slf4j
public class UdpProtocolParser {
    
    /**
     * 协议头长度（字节）
     * 1字节（版本+帧类型+保留）+ 1字节（标志位）+ 2字节（序列号）+ 2字节（数据长度）+ 4字节（时间戳）= 10字节
     */
    public static final int HEADER_LENGTH = 10;
    
    /**
     * 最大数据长度（65535字节）
     */
    public static final int MAX_DATA_LENGTH = 65535;
    
    /**
     * 解析字节流为协议消息
     * 
     * @param byteBuf 字节缓冲区
     * @return 协议消息对象，如果解析失败返回null
     */
    public static UdpProtocolMessage parse(ByteBuf byteBuf) {
        if (byteBuf == null || byteBuf.readableBytes() < HEADER_LENGTH) {
            log.warn("数据长度不足，无法解析协议头。需要至少{}字节，实际{}字节", 
                    HEADER_LENGTH, byteBuf != null ? byteBuf.readableBytes() : 0);
            return null;
        }
        
        try {
            // 保存读取位置
            int readerIndex = byteBuf.readerIndex();
            
            // 读取第一个字节（版本+帧类型+保留）
            byte byte0 = byteBuf.readByte();
            
            // 解析版本（高2位）
            int version = (byte0 >> 6) & 0x03;
            
            // 解析帧类型（中间4位）
            int frameTypeCode = (byte0 >> 2) & 0x0F;
            UdpProtocolHeader.FrameType frameType;
            try {
                frameType = UdpProtocolHeader.FrameType.fromCode(frameTypeCode);
            } catch (IllegalArgumentException e) {
                log.warn("未知的帧类型: {}", frameTypeCode);
                byteBuf.readerIndex(readerIndex);
                return null;
            }
            
            // 解析保留字段（低2位）
            int reserved = byte0 & 0x03;
            
            // 读取标志位（1字节）
            byte flags = byteBuf.readByte();
            
            // 读取序列号（2字节，大端序）
            int sequence = byteBuf.readUnsignedShort();
            
            // 读取数据长度（2字节，大端序）
            int dataLength = byteBuf.readUnsignedShort();
            
            // 验证数据长度
            if (dataLength > MAX_DATA_LENGTH) {
                log.warn("数据长度超出限制: {}，最大允许: {}", dataLength, MAX_DATA_LENGTH);
                byteBuf.readerIndex(readerIndex);
                return null;
            }
            
            // 读取时间戳（4字节，大端序）
            // 注意：时间戳在数据长度之后，净荷数据之前
            if (byteBuf.readableBytes() < 4) {
                log.warn("数据不完整，无法读取时间戳。需要4字节，实际剩余{}字节", 
                        byteBuf.readableBytes());
                byteBuf.readerIndex(readerIndex);
                return null;
            }
            long timestamp = byteBuf.readUnsignedInt();
            
            // 检查是否有足够的数据来读取净荷
            if (byteBuf.readableBytes() < dataLength) {
                log.warn("数据不完整。需要{}字节净荷数据，实际剩余{}字节", 
                        dataLength, byteBuf.readableBytes());
                byteBuf.readerIndex(readerIndex);
                return null;
            }
            
            // 读取净荷数据
            byte[] payload = null;
            if (dataLength > 0) {
                payload = new byte[dataLength];
                byteBuf.readBytes(payload);
            }
            
            // 构建协议头
            UdpProtocolHeader header = new UdpProtocolHeader();
            header.setVersion(version);
            header.setFrameType(frameType);
            header.setReserved(reserved);
            header.setFlags(flags);
            header.setSequence(sequence);
            header.setDataLength(dataLength);
            header.setTimestamp(timestamp);
            
            // 构建协议消息
            UdpProtocolMessage message = new UdpProtocolMessage(header, payload);
            
            // 解析净荷实体类
            if (payload != null && payload.length > 0) {
                Object payloadEntity = PayloadParser.parsePayload(frameType, payload);
                message.setPayloadEntity(payloadEntity);
                if (payloadEntity != null) {
                    log.debug("净荷解析成功: frameType={}, payloadEntity={}", frameType, payloadEntity.getClass().getSimpleName());
                } else {
                    log.debug("净荷解析失败或为空: frameType={}", frameType);
                }
            }
            
            // 保存原始数据（用于调试）
            byteBuf.readerIndex(readerIndex);
            byte[] rawData = new byte[HEADER_LENGTH + dataLength];
            byteBuf.readBytes(rawData);
            message.setRawData(rawData);
            
            log.debug("协议解析成功: version={}, frameType={}, sequence={}, dataLength={}", 
                    version, frameType, sequence, dataLength);
            
            return message;
        } catch (Exception e) {
            log.error("解析协议消息失败", e);
            return null;
        }
    }
    
    /**
     * 验证协议消息的完整性
     * 
     * @param byteBuf 字节缓冲区
     * @return 是否完整
     */
    public static boolean isComplete(ByteBuf byteBuf) {
        if (byteBuf == null || byteBuf.readableBytes() < HEADER_LENGTH) {
            return false;
        }
        
        try {
            int readerIndex = byteBuf.readerIndex();
            
            // 读取数据长度字段（跳过前面的5个字节：版本+帧类型+保留(1) + 标志位(1) + 序列号(2) + 数据长度(2)）
            if (byteBuf.readableBytes() < 6) {
                return false;
            }
            byteBuf.skipBytes(5);
            int dataLength = byteBuf.readUnsignedShort();
            
            // 恢复读取位置
            byteBuf.readerIndex(readerIndex);
            
            // 检查总长度：协议头(10字节) + 净荷数据长度
            int totalLength = HEADER_LENGTH + dataLength;
            return byteBuf.readableBytes() >= totalLength;
        } catch (Exception e) {
            log.error("验证协议完整性失败", e);
            return false;
        }
    }
}

