package com.mayday.common.protocol.udp.payload;

import com.mayday.common.protocol.udp.annotation.ProtocolEntity;
import com.mayday.common.protocol.udp.annotation.ProtocolField;
import lombok.Data;

/**
 * 心跳包净荷
 * 
 * <p>示例：</p>
 * <pre>
 * 字节流示例（5字节）：
 * +--------+--------+--------+--------+--------+
 * | 设备ID (4字节)          | 状态(1字节) |
 * +--------+--------+--------+--------+--------+
 * | 00 00 00 01             | 00       |
 * +--------+--------+--------+--------+--------+
 * 
 * 解析结果：
 * - 设备ID: 1 (0x00000001)
 * - 状态: 0 (0x00, 正常)
 * </pre>
 * 
 * <p>自定义净荷类示例：</p>
 * <pre>
 * {@code
 * @ProtocolEntity(name = "自定义心跳净荷", description = "自定义心跳净荷数据", version = 1)
 * public class CustomHeartbeatPayload {
 *     @ProtocolField(name = "设备ID", description = "设备ID", byteOffset = 0, byteLength = 4, type = ProtocolField.FieldType.INT)
 *     private Integer deviceId;
 *     
 *     @ProtocolField(name = "温度", description = "温度", byteOffset = 4, byteLength = 2, type = ProtocolField.FieldType.SHORT)
 *     private Integer temperature;
 * }
 * }
 * </pre>
 * 
 * @author mayday
 */
@Data
@ProtocolEntity(name = "心跳包净荷", description = "心跳包净荷数据", version = 1)
public class HeartbeatPayload {
    
    /**
     * 设备ID（可选，4字节）
     */
    @ProtocolField(name = "设备ID", description = "设备ID", byteOffset = 0, byteLength = 4, type = ProtocolField.FieldType.INT, required = false)
    private Integer deviceId;
    
    /**
     * 状态信息（可选，1字节）
     * 0x00: 正常
     * 0x01: 警告
     * 0x02: 错误
     */
    @ProtocolField(name = "状态", description = "状态", byteOffset = 4, byteLength = 1, type = ProtocolField.FieldType.BYTE, required = false)
    private Byte status;
}

