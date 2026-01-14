package com.mayday.common.protocol.udp.payload;

import com.mayday.common.protocol.udp.annotation.ProtocolEntity;
import com.mayday.common.protocol.udp.annotation.ProtocolField;
import lombok.Data;

/**
 * 数据包净荷
 * 
 * <p>示例：</p>
 * <pre>
 * 字节流示例（8字节）：
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * | 数据类型(2字节) | 数据内容(变长)                                    |
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * | 00 01          | 48 65 6C 6C 6F                                  |
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * 
 * 解析结果：
 * - 数据类型: 1 (0x0001, 传感器数据)
 * - 数据内容: "Hello" (0x48 65 6C 6C 6F)
 * </pre>
 * 
 * <p>自定义净荷类示例：</p>
 * <pre>
 * {@code
 * @ProtocolEntity(name = "传感器数据净荷", description = "传感器数据净荷", version = 1)
 * public class SensorDataPayload {
 *     @ProtocolField(name = "传感器ID", description = "传感器ID", byteOffset = 0, byteLength = 2, type = ProtocolField.FieldType.SHORT)
 *     private Integer sensorId;
 *     
 *     @ProtocolField(name = "温度值", description = "温度值", byteOffset = 2, byteLength = 2, type = ProtocolField.FieldType.SHORT)
 *     private Integer temperature;
 *     
 *     @ProtocolField(name = "湿度值", description = "湿度值", byteOffset = 4, byteLength = 2, type = ProtocolField.FieldType.SHORT)
 *     private Integer humidity;
 * }
 * }
 * </pre>
 * 
 * @author mayday
 */
@Data
@ProtocolEntity(name = "数据包净荷", description = "数据包净荷数据", version = 1)
public class DataPayload {
    
    /**
     * 数据类型（2字节）
     * 0x0001: 传感器数据
     * 0x0002: 位置信息
     * 0x0003: 状态上报
     * 0x0004-0xFFFF: 自定义类型
     */
    @ProtocolField(name = "数据类型", description = "数据类型", byteOffset = 0, byteLength = 2, type = ProtocolField.FieldType.SHORT)
    private Integer dataType;
    
    /**
     * 数据内容（变长）
     */
    @ProtocolField(name = "数据内容", description = "数据内容", byteOffset = 2, type = ProtocolField.FieldType.BYTES)
    private byte[] data;
}

