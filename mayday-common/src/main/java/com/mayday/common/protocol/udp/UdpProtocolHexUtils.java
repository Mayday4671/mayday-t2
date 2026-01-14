package com.mayday.common.protocol.udp;

import lombok.extern.slf4j.Slf4j;

/**
 * UDP协议16进制工具类
 * 用于16进制字符串和字节数组之间的转换
 * 
 * @author mayday
 */
@Slf4j
public class UdpProtocolHexUtils {
    
    /**
     * 将16进制字符串转换为字节数组
     * 支持空格、冒号、连字符分隔符，也支持无分隔符
     * 
     * @param hexString 16进制字符串，例如：
     *                  "40 00 00 01 00 05 00 00 00 00 48 65 6C 6C 6F"
     *                  "40:00:00:01:00:05:00:00:00:00:48:65:6C:6C:6F"
     *                  "4000000100050000000048656C6C6F"
     * @return 字节数组
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.trim().isEmpty()) {
            return new byte[0];
        }
        
        // 移除所有分隔符（空格、冒号、连字符）
        String cleanHex = hexString.replaceAll("[\\s:,-]", "").toUpperCase();
        
        // 验证16进制字符串长度（必须是偶数）
        if (cleanHex.length() % 2 != 0) {
            throw new IllegalArgumentException("16进制字符串长度必须是偶数: " + hexString);
        }
        
        // 验证16进制字符
        if (!cleanHex.matches("[0-9A-F]+")) {
            throw new IllegalArgumentException("包含非16进制字符: " + hexString);
        }
        
        // 转换为字节数组
        byte[] bytes = new byte[cleanHex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(cleanHex.substring(index, index + 2), 16);
        }
        
        return bytes;
    }
    
    /**
     * 将字节数组转换为16进制字符串
     * 
     * @param bytes 字节数组
     * @param separator 分隔符（可选，如空格、冒号等）
     * @return 16进制字符串
     */
    public static String bytesToHexString(byte[] bytes, String separator) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && separator != null && !separator.isEmpty()) {
                sb.append(separator);
            }
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        
        return sb.toString();
    }
    
    /**
     * 将字节数组转换为16进制字符串（无分隔符）
     */
    public static String bytesToHexString(byte[] bytes) {
        return bytesToHexString(bytes, null);
    }
    
    /**
     * 将字节数组转换为16进制字符串（空格分隔）
     */
    public static String bytesToHexStringWithSpace(byte[] bytes) {
        return bytesToHexString(bytes, " ");
    }
    
    /**
     * 将字节数组转换为16进制字符串（冒号分隔）
     */
    public static String bytesToHexStringWithColon(byte[] bytes) {
        return bytesToHexString(bytes, ":");
    }
    
    /**
     * 格式化16进制字符串（每8个字节换行）
     */
    public static String formatHexString(byte[] bytes, int bytesPerLine) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && i % bytesPerLine == 0) {
                sb.append("\n");
            }
            if (i > 0 && i % bytesPerLine != 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        
        return sb.toString();
    }
    
    /**
     * 构建16进制协议消息
     * 
     * @param version 版本（0-3）
     * @param frameType 帧类型（0-15）
     * @param flags 标志位
     * @param sequence 序列号
     * @param payloadHex 净荷16进制字符串
     * @return 16进制字符串
     */
    public static String buildHexMessage(int version, int frameType, byte flags, 
                                         int sequence, String payloadHex) {
        UdpProtocolHeader header = new UdpProtocolHeader();
        header.setVersion(version);
        header.setFrameType(UdpProtocolHeader.FrameType.fromCode(frameType));
        header.setFlags(flags);
        header.setSequence(sequence);
        
        byte[] payload = payloadHex != null && !payloadHex.isEmpty() 
                ? hexStringToBytes(payloadHex) 
                : null;
        
        UdpProtocolMessage message = new UdpProtocolMessage(header, payload);
        byte[] data = UdpProtocolBuilder.build(message);
        
        return bytesToHexStringWithSpace(data);
    }
    
    /**
     * 解析16进制协议消息
     * 
     * @param hexString 16进制字符串
     * @return 协议消息对象
     */
    public static UdpProtocolMessage parseHexMessage(String hexString) {
        byte[] bytes = hexStringToBytes(hexString);
        io.netty.buffer.ByteBuf byteBuf = io.netty.buffer.Unpooled.wrappedBuffer(bytes);
        return UdpProtocolParser.parse(byteBuf);
    }
    
    /**
     * 打印协议消息的详细信息（16进制格式）
     */
    public static void printProtocolMessage(UdpProtocolMessage message) {
        if (message == null) {
            log.warn("协议消息为空");
            return;
        }
        
        UdpProtocolHeader header = message.getHeader();
        
        log.info("========== UDP协议消息（16进制） ==========");
        log.info("原始数据: {}", bytesToHexStringWithSpace(message.getRawData()));
        String binaryVersion = Integer.toBinaryString(header.getVersion());
        log.info("协议版本: {} (二进制: {})", header.getVersion(), 
                String.format("%2s", binaryVersion).replace(' ', '0'));
        log.info("帧类型: {} ({}, 代码: 0x{})", 
                header.getFrameType(), 
                header.getFrameType().getDescription(),
                Integer.toHexString(header.getFrameType().getCode()).toUpperCase());
        log.info("序列号: {} (0x{})", header.getSequence(), 
                String.format("%04X", header.getSequence()));
        log.info("数据长度: {} bytes (0x{})", header.getDataLength(), 
                String.format("%04X", header.getDataLength()));
        log.info("时间戳: {} (0x{})", header.getTimestamp(), 
                String.format("%08X", header.getTimestamp()));
        String binaryFlags = Integer.toBinaryString(header.getFlags() & 0xFF);
        log.info("标志位: 0x{} (二进制: {})", 
                String.format("%02X", header.getFlags()),
                String.format("%8s", binaryFlags).replace(' ', '0'));
        log.info("  - 需要响应: {}", UdpProtocolHeader.Flags.isNeedResponse(header.getFlags()));
        log.info("  - 加密: {}", UdpProtocolHeader.Flags.isEncrypted(header.getFlags()));
        log.info("  - 压缩: {}", UdpProtocolHeader.Flags.isCompressed(header.getFlags()));
        log.info("  - 分片: {}", UdpProtocolHeader.Flags.isFragmented(header.getFlags()));
        
        if (message.getPayload() != null && message.getPayload().length > 0) {
            log.info("净荷（16进制）: {}", bytesToHexStringWithSpace(message.getPayload()));
            log.info("净荷（字符串）: {}", message.getPayloadAsString());
        } else {
            log.info("净荷: 空");
        }
        log.info("==========================================");
    }
}

