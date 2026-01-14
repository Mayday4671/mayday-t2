package com.mayday.common.protocol.udp.payload;

import com.mayday.common.protocol.udp.annotation.ProtocolEntity;
import com.mayday.common.protocol.udp.annotation.ProtocolField;
import lombok.Data;

/**
 * 控制包净荷
 * 
 * <p>示例：</p>
 * <pre>
 * 字节流示例（7字节）：
 * +--------+--------+--------+--------+--------+--------+--------+
 * | 控制命令(2字节) | 命令参数(变长)                              |
 * +--------+--------+--------+--------+--------+--------+--------+
 * | 00 01          | 53 74 61 72 74                            |
 * +--------+--------+--------+--------+--------+--------+--------+
 * 
 * 解析结果：
 * - 控制命令: 1 (0x0001, 启动)
 * - 命令参数: "Start" (0x53 74 61 72 74)
 * </pre>
 * 
 * <p>自定义净荷类示例：</p>
 * <pre>
 * {@code
 * @ProtocolEntity(name = "配置控制净荷", description = "配置控制净荷", version = 1)
 * public class ConfigControlPayload {
 *     @ProtocolField(name = "配置项ID", description = "配置项ID", byteOffset = 0, byteLength = 2, type = ProtocolField.FieldType.SHORT)
 *     private Integer configId;
 *     
 *     @ProtocolField(name = "配置值", description = "配置值", byteOffset = 2, byteLength = 4, type = ProtocolField.FieldType.INT)
 *     private Integer configValue;
 * }
 * }
 * </pre>
 * 
 * @author mayday
 */
@Data
@ProtocolEntity(name = "控制包净荷", description = "控制包净荷数据", version = 1)
public class ControlPayload {
    
    /**
     * 控制命令（2字节）
     * 0x0001: 启动
     * 0x0002: 停止
     * 0x0003: 重启
     * 0x0004: 配置
     * 0x0005-0xFFFF: 自定义命令
     */
    @ProtocolField(name = "控制命令", description = "控制命令", byteOffset = 0, byteLength = 2, type = ProtocolField.FieldType.SHORT)
    private Integer command;
    
    /**
     * 命令参数（变长）
     */
    @ProtocolField(name = "命令参数", description = "命令参数", byteOffset = 2, type = ProtocolField.FieldType.BYTES)
    private byte[] parameters;
}

