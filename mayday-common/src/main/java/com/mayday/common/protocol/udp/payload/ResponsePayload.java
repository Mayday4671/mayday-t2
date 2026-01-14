package com.mayday.common.protocol.udp.payload;

import com.mayday.common.protocol.udp.annotation.ProtocolEntity;
import com.mayday.common.protocol.udp.annotation.ProtocolField;
import lombok.Data;

/**
 * 响应包净荷
 * 
 * <p>示例：</p>
 * <pre>
 * 字节流示例（9字节）：
 * +--------+--------+--------+--------+--------+--------+--------+--------+--------+
 * | 响应码(2字节)   | 响应消息(变长)                                          |
 * +--------+--------+--------+--------+--------+--------+--------+--------+--------+
 * | 00 00          | 4F 4B                                                  |
 * +--------+--------+--------+--------+--------+--------+--------+--------+--------+
 * 
 * 解析结果：
 * - 响应码: 0 (0x0000, 成功)
 * - 响应消息: "OK" (0x4F 4B)
 * </pre>
 * 
 * <p>自定义净荷类示例：</p>
 * <pre>
 * {@code
 * @ProtocolEntity(name = "详细响应净荷", description = "详细响应净荷", version = 1)
 * public class DetailedResponsePayload {
 *     @ProtocolField(name = "响应码", description = "响应码", byteOffset = 0, byteLength = 2, type = ProtocolField.FieldType.SHORT)
 *     private Integer responseCode;
 *     
 *     @ProtocolField(name = "错误码", description = "错误码", byteOffset = 2, byteLength = 2, type = ProtocolField.FieldType.SHORT)
 *     private Integer errorCode;
 *     
 *     @ProtocolField(name = "响应数据", description = "响应数据", byteOffset = 4, type = ProtocolField.FieldType.BYTES)
 *     private byte[] responseData;
 * }
 * }
 * </pre>
 * 
 * @author mayday
 */
@Data
@ProtocolEntity(name = "响应包净荷", description = "响应包净荷数据", version = 1)
public class ResponsePayload {
    
    /**
     * 响应码（2字节）
     * 0x0000: 成功
     * 0x0001: 失败
     * 0x0002: 参数错误
     * 0x0003: 权限不足
     * 0x0004-0xFFFF: 自定义错误码
     */
    @ProtocolField(name = "响应码", description = "响应码", byteOffset = 0, byteLength = 2, type = ProtocolField.FieldType.SHORT)
    private Integer responseCode;
    
    /**
     * 响应消息（变长）
     */
    @ProtocolField(name = "响应消息", description = "响应消息", byteOffset = 2, type = ProtocolField.FieldType.BYTES)
    private byte[] message;
}

