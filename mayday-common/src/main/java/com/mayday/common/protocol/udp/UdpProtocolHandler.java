package com.mayday.common.protocol.udp;

import com.mayday.common.netty.AbstractMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * UDP协议处理器
 * 统一处理UDP协议消息的解析和响应
 * 
 * 使用方式：
 * 在Netty服务器配置中，将消息处理器类设置为：com.mayday.common.protocol.udp.UdpProtocolHandler
 * 
 * @author mayday
 */
@Slf4j
public class UdpProtocolHandler extends AbstractMessageHandler {
    
    private static int sequenceCounter = 0;
    
    @Override
    protected Object handleByteBuf(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        try {
            // 1. 解析协议消息
            UdpProtocolMessage message = UdpProtocolParser.parse(byteBuf);
            if (message == null) {
                log.warn("协议解析失败，忽略消息");
                return null;
            }
            
            UdpProtocolHeader header = message.getHeader();
            String clientAddress = ctx.channel().remoteAddress() != null 
                    ? ctx.channel().remoteAddress().toString() 
                    : "unknown";
            
            // 使用16进制工具类打印详细信息
            UdpProtocolHexUtils.printProtocolMessage(message);
            log.info("客户端地址: {}", clientAddress);
            
            // 2. 根据帧类型处理
            Object response = processByFrameType(ctx, message, clientAddress);
            
            // 3. 如果需要响应，构建响应消息
            if (UdpProtocolHeader.Flags.isNeedResponse(header.getFlags())) {
                if (response == null) {
                    response = "处理成功";
                }
                
                byte[] responsePayload;
                if (response instanceof String) {
                    responsePayload = ((String) response).getBytes();
                } else if (response instanceof byte[]) {
                    responsePayload = (byte[]) response;
                } else {
                    responsePayload = response.toString().getBytes();
                }
                
                UdpProtocolMessage responseMessage = UdpProtocolBuilder.buildResponse(
                        header.getSequence(), responsePayload);
                
                byte[] responseBytes = UdpProtocolBuilder.build(responseMessage);
                
                // 返回ByteBuf对象
                return Unpooled.wrappedBuffer(responseBytes);
            }
            
            return null;
        } catch (Exception e) {
            log.error("处理UDP协议消息失败", e);
            return null;
        }
    }
    
    /**
     * 根据帧类型处理消息
     */
    private Object processByFrameType(ChannelHandlerContext ctx, UdpProtocolMessage message, String clientAddress) {
        UdpProtocolHeader.FrameType frameType = message.getHeader().getFrameType();
        
        switch (frameType) {
            case HEARTBEAT:
                return processHeartbeat(message, clientAddress);
            case DATA:
                return processData(message, clientAddress);
            case CONTROL:
                return processControl(message, clientAddress);
            case RESPONSE:
                return processResponse(message, clientAddress);
            default:
                log.warn("未知的帧类型: {}", frameType);
                return "未知的帧类型";
        }
    }
    
    /**
     * 处理心跳包
     */
    private Object processHeartbeat(UdpProtocolMessage message, String clientAddress) {
        log.info("收到心跳包，序列号: {}", message.getHeader().getSequence());
        return "pong";
    }
    
    /**
     * 处理数据包
     */
    private Object processData(UdpProtocolMessage message, String clientAddress) {
        log.info("收到数据包，序列号: {}, 数据: {}", 
                message.getHeader().getSequence(), message.getPayloadAsString());
        
        // TODO: 在这里添加数据包的业务处理逻辑
        // 例如：保存数据、转发数据、处理业务逻辑等
        
        return "数据已接收";
    }
    
    /**
     * 处理控制包
     */
    private Object processControl(UdpProtocolMessage message, String clientAddress) {
        log.info("收到控制包，序列号: {}, 数据: {}", 
                message.getHeader().getSequence(), message.getPayloadAsString());
        
        // TODO: 在这里添加控制包的处理逻辑
        // 例如：服务器配置、状态查询、控制命令等
        
        String controlData = message.getPayloadAsString();
        if (controlData != null) {
            if (controlData.startsWith("QUERY_STATUS")) {
                return "服务器状态正常";
            } else if (controlData.startsWith("SET_CONFIG")) {
                return "配置已更新";
            }
        }
        
        return "控制命令已执行";
    }
    
    /**
     * 处理响应包
     */
    private Object processResponse(UdpProtocolMessage message, String clientAddress) {
        log.info("收到响应包，序列号: {}, 数据: {}", 
                message.getHeader().getSequence(), message.getPayloadAsString());
        
        // 响应包通常不需要再响应
        return null;
    }
}

