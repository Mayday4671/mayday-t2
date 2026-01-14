package com.mayday.common.protocol.udp;

import com.mayday.common.protocol.udp.annotation.ProtocolEntity;
import com.mayday.common.protocol.udp.annotation.ProtocolField;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 协议元数据提取器
 * 用于提取协议实体类的元数据，支持前端展示和解析
 * 
 * @author mayday
 */
@Slf4j
public class ProtocolMetadataExtractor {
    
    /**
     * 提取协议消息的元数据
     * 
     * @param message 协议消息
     * @return 协议实体元数据
     */
    public static ProtocolEntityMetadata extractMetadata(UdpProtocolMessage message) {
        if (message == null) {
            return null;
        }
        
        ProtocolEntityMetadata metadata = new ProtocolEntityMetadata();
        
        // 提取协议消息的注解信息
        ProtocolEntity entityAnnotation = UdpProtocolMessage.class.getAnnotation(ProtocolEntity.class);
        if (entityAnnotation != null) {
            metadata.setName(entityAnnotation.name());
            metadata.setDescription(entityAnnotation.description());
            metadata.setVersion(1); // 默认版本为1
        } else {
            metadata.setName("UDP协议消息");
            metadata.setDescription("UDP协议消息");
            metadata.setVersion(1);
        }
        
        // 提取原始16进制数据
        if (message.getRawData() != null) {
            metadata.setHexData(bytesToHexString(message.getRawData()));
        }
        
        // 提取协议头的元数据
        if (message.getHeader() != null) {
            List<ProtocolFieldMetadata> headerFields = extractFieldMetadata(message.getHeader());
            metadata.getFields().addAll(headerFields);
        }
        
        // 提取净荷的元数据（优先提取净荷实体类字段，如果没有则逐字节拆分）
        metadata.getFields().addAll(extractPayloadMetadata(message));
        
        // 计算总长度
        if (metadata.getTotalLength() < 0) {
            int totalLength = UdpProtocolParser.HEADER_LENGTH; // 协议头固定10字节
            if (message.getPayload() != null) {
                totalLength += message.getPayload().length;
            }
            metadata.setTotalLength(totalLength);
        }
        
        return metadata;
    }
    
    /**
     * 提取对象的字段元数据
     */
    private static List<ProtocolFieldMetadata> extractFieldMetadata(Object obj) {
        List<ProtocolFieldMetadata> fields = new ArrayList<>();
        
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            ProtocolField fieldAnnotation = field.getAnnotation(ProtocolField.class);
            if (fieldAnnotation != null) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    
                    ProtocolFieldMetadata fieldMetadata = new ProtocolFieldMetadata(fieldAnnotation, value);
                    
                    // 根据字段类型设置不同的展示值
                    if (value != null) {
                        if (value instanceof byte[]) {
                            // 字节数组类型
                            byte[] bytes = (byte[]) value;
                            fieldMetadata.setHexValue(bytesToHexString(bytes));
                            fieldMetadata.setStringValue(new String(bytes));
                            fieldMetadata.setValue(bytes);
                        } else if (value instanceof Byte) {
                            byte byteValue = (Byte) value;
                            fieldMetadata.setHexValue(String.format("%02X", byteValue & 0xFF));
                            fieldMetadata.setStringValue(String.valueOf(byteValue));
                            fieldMetadata.setBinaryValue(String.format("%8s", Integer.toBinaryString(byteValue & 0xFF)).replace(' ', '0'));
                        } else if (value instanceof Integer) {
                            int intValue = (Integer) value;
                            fieldMetadata.setHexValue(String.format("%04X", intValue));
                            fieldMetadata.setStringValue(String.valueOf(intValue));
                            fieldMetadata.setBinaryValue(Integer.toBinaryString(intValue));
                        } else if (value instanceof Long) {
                            long longValue = (Long) value;
                            fieldMetadata.setHexValue(String.format("%08X", longValue));
                            fieldMetadata.setStringValue(String.valueOf(longValue));
                            fieldMetadata.setBinaryValue(Long.toBinaryString(longValue));
                        } else if (value instanceof UdpProtocolHeader.FrameType) {
                            UdpProtocolHeader.FrameType frameType = (UdpProtocolHeader.FrameType) value;
                            fieldMetadata.setStringValue(frameType.getDescription());
                            fieldMetadata.setHexValue(String.format("%02X", frameType.getCode()));
                        } else {
                            fieldMetadata.setStringValue(value.toString());
                        }
                    }
                    
                    fields.add(fieldMetadata);
                } catch (IllegalAccessException e) {
                    log.warn("无法访问字段: {}", field.getName(), e);
                }
            }
        }
        
        return fields;
    }
    
    /**
     * 提取净荷字段元数据：优先提取净荷实体类字段，如果没有则逐字节拆分
     */
    private static List<ProtocolFieldMetadata> extractPayloadMetadata(UdpProtocolMessage message) {
        List<ProtocolFieldMetadata> fields = new ArrayList<>();
        byte[] payload = message.getPayload();
        int payloadLength = payload != null ? payload.length : 0;
        int payloadOffset = UdpProtocolParser.HEADER_LENGTH;
        
        // 优先提取净荷实体类的字段
        Object payloadEntity = message.getPayloadEntity();
        if (payloadEntity != null) {
            // 提取净荷实体类的字段元数据
            List<ProtocolFieldMetadata> payloadFields = extractFieldMetadata(payloadEntity);
            // 调整字节偏移量（加上协议头长度）
            for (ProtocolFieldMetadata field : payloadFields) {
                field.setByteOffset(field.getByteOffset() + payloadOffset);
            }
            fields.addAll(payloadFields);
            return fields;
        }
        
        // 如果没有净荷实体类，使用原来的逐字节拆分逻辑
        String payloadHex = payloadLength > 0 ? bytesToHexString(payload) : "";
        String payloadString = payloadLength > 0 ? message.getPayloadAsString() : "";
        
        String payloadName;
        String payloadDesc;
        switch (message.getHeader() != null ? message.getHeader().getFrameType() : null) {
            case HEARTBEAT -> {
                payloadName = "心跳负载";
                payloadDesc = "心跳保活，无业务数据";
            }
            case DATA -> {
                payloadName = "业务数据";
                payloadDesc = "应用层数据内容";
            }
            case CONTROL -> {
                payloadName = "控制指令";
                payloadDesc = "控制命令与参数";
            }
            case RESPONSE -> {
                payloadName = "响应内容";
                payloadDesc = "响应结果数据";
            }
            default -> {
                payloadName = "数据载荷";
                payloadDesc = "净荷内容";
            }
        }
        
        // 摘要字段
        ProtocolFieldMetadata summary = new ProtocolFieldMetadata();
        summary.setName(payloadName);
        summary.setDescription(payloadDesc);
        summary.setByteOffset(payloadOffset);
        summary.setByteLength(payloadLength);
        summary.setType(ProtocolField.FieldType.BYTES);
        summary.setFormat("16进制/字符串");
        summary.setValue(payload);
        summary.setHexValue(payloadHex);
        summary.setStringValue(payloadString);
        fields.add(summary);
        
        // 逐字节拆分（便于前端按字节位展示）
        if (payloadLength > 0) {
            for (int i = 0; i < payloadLength; i++) {
                ProtocolFieldMetadata byteField = new ProtocolFieldMetadata();
                byteField.setName(payloadName + "字节#" + i);
                byteField.setDescription("净荷第" + i + "字节");
                byteField.setByteOffset(payloadOffset + i);
                byteField.setByteLength(1);
                byteField.setType(ProtocolField.FieldType.BYTE);
                byteField.setFormat("16进制");
                byteField.setValue(payload[i]);
                byteField.setHexValue(String.format("%02X", payload[i] & 0xFF));
                if (payload[i] >= 32 && payload[i] <= 126) {
                    byteField.setStringValue(String.valueOf((char) payload[i]));
                } else {
                    byteField.setStringValue("");
                }
                fields.add(byteField);
            }
        }
        
        return fields;
    }
    
    /**
     * 将字节数组转换为16进制字符串
     */
    private static String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}

