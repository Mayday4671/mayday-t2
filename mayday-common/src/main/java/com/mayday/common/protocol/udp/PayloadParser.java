package com.mayday.common.protocol.udp;

import com.mayday.common.protocol.udp.annotation.ProtocolField;
import com.mayday.common.protocol.udp.payload.*;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * 净荷解析器
 * 根据帧类型解析不同的净荷实体类
 * 
 * @author mayday
 */
@Slf4j
public class PayloadParser {
    
    /**
     * 解析净荷为对应的实体类
     * 
     * @param frameType 帧类型
     * @param payloadBytes 净荷字节数组
     * @return 净荷实体对象，如果解析失败返回null
     */
    public static Object parsePayload(UdpProtocolHeader.FrameType frameType, byte[] payloadBytes) {
        if (payloadBytes == null || payloadBytes.length == 0) {
            return null;
        }
        
        try {
            ByteBuf byteBuf = io.netty.buffer.Unpooled.wrappedBuffer(payloadBytes);
            
            switch (frameType) {
                case HEARTBEAT:
                    return parseHeartbeatPayload(byteBuf);
                case DATA:
                    return parseDataPayload(byteBuf);
                case CONTROL:
                    return parseControlPayload(byteBuf);
                case RESPONSE:
                    return parseResponsePayload(byteBuf);
                default:
                    log.warn("未知的帧类型: {}", frameType);
                    return null;
            }
        } catch (Exception e) {
            log.error("解析净荷失败: frameType={}, error={}", frameType, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 解析心跳包净荷
     */
    private static HeartbeatPayload parseHeartbeatPayload(ByteBuf byteBuf) {
        HeartbeatPayload payload = new HeartbeatPayload();
        
        if (byteBuf.readableBytes() >= 4) {
            payload.setDeviceId(byteBuf.readInt());
        }
        
        if (byteBuf.readableBytes() >= 1) {
            payload.setStatus(byteBuf.readByte());
        }
        
        return payload;
    }
    
    /**
     * 解析数据包净荷
     */
    private static DataPayload parseDataPayload(ByteBuf byteBuf) {
        DataPayload payload = new DataPayload();
        
        if (byteBuf.readableBytes() >= 2) {
            payload.setDataType(byteBuf.readUnsignedShort());
            
            // 读取剩余数据（变长字段）
            int remainingBytes = byteBuf.readableBytes();
            if (remainingBytes > 0) {
                byte[] data = new byte[remainingBytes];
                byteBuf.readBytes(data);
                payload.setData(data);
            } else {
                payload.setData(new byte[0]);
            }
        }
        
        return payload;
    }
    
    /**
     * 解析控制包净荷
     */
    private static ControlPayload parseControlPayload(ByteBuf byteBuf) {
        ControlPayload payload = new ControlPayload();
        
        if (byteBuf.readableBytes() >= 2) {
            payload.setCommand(byteBuf.readUnsignedShort());
            
            // 读取剩余参数（变长字段）
            int remainingBytes = byteBuf.readableBytes();
            if (remainingBytes > 0) {
                byte[] parameters = new byte[remainingBytes];
                byteBuf.readBytes(parameters);
                payload.setParameters(parameters);
            } else {
                payload.setParameters(new byte[0]);
            }
        }
        
        return payload;
    }
    
    /**
     * 解析响应包净荷
     */
    private static ResponsePayload parseResponsePayload(ByteBuf byteBuf) {
        ResponsePayload payload = new ResponsePayload();
        
        if (byteBuf.readableBytes() >= 2) {
            payload.setResponseCode(byteBuf.readUnsignedShort());
            
            // 读取剩余消息（变长字段）
            int remainingBytes = byteBuf.readableBytes();
            if (remainingBytes > 0) {
                byte[] message = new byte[remainingBytes];
                byteBuf.readBytes(message);
                payload.setMessage(message);
            } else {
                payload.setMessage(new byte[0]);
            }
        }
        
        return payload;
    }
    
    /**
     * 使用反射解析净荷（通用方法，支持自定义净荷类）
     */
    public static Object parsePayloadByReflection(Class<?> payloadClass, byte[] payloadBytes) {
        if (payloadClass == null || payloadBytes == null || payloadBytes.length == 0) {
            return null;
        }
        
        try {
            Object payload = payloadClass.getDeclaredConstructor().newInstance();
            ByteBuf byteBuf = io.netty.buffer.Unpooled.wrappedBuffer(payloadBytes);
            
            Field[] fields = payloadClass.getDeclaredFields();
            for (Field field : fields) {
                ProtocolField annotation = field.getAnnotation(ProtocolField.class);
                if (annotation == null) {
                    continue;
                }
                
                field.setAccessible(true);
                int byteOffset = annotation.byteOffset();
                int byteLength = annotation.byteLength();
                
                // 跳过到指定偏移
                if (byteBuf.readerIndex() < byteOffset) {
                    byteBuf.skipBytes(byteOffset - byteBuf.readerIndex());
                }
                
                // 检查是否有足够的数据
                if (byteBuf.readableBytes() < byteLength) {
                    log.warn("净荷数据不足，无法解析字段: {}, 需要{}字节，实际{}字节", 
                            field.getName(), byteLength, byteBuf.readableBytes());
                    continue;
                }
                
                // 根据字段类型读取数据
                ProtocolField.FieldType fieldType = annotation.type();
                switch (fieldType) {
                    case BYTE:
                        field.set(payload, byteBuf.readByte());
                        break;
                    case SHORT:
                        field.set(payload, byteBuf.readUnsignedShort());
                        break;
                    case INT:
                        field.set(payload, byteBuf.readInt());
                        break;
                    case LONG:
                        field.set(payload, byteBuf.readLong());
                        break;
                    case BYTES:
                        // 变长字段，读取剩余所有字节
                        int remainingBytes = byteBuf.readableBytes();
                        if (remainingBytes > 0) {
                            byte[] bytes = new byte[remainingBytes];
                            byteBuf.readBytes(bytes);
                            field.set(payload, bytes);
                        }
                        break;
                    default:
                        log.warn("不支持的字段类型: {}", fieldType);
                        break;
                }
            }
            
            return payload;
        } catch (Exception e) {
            log.error("反射解析净荷失败: payloadClass={}, error={}", payloadClass.getName(), e.getMessage(), e);
            return null;
        }
    }
}

