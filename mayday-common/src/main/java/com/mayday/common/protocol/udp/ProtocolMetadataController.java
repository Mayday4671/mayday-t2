package com.mayday.common.protocol.udp;

import com.mayday.common.web.AjaxResult;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 协议元数据控制器
 * 提供协议解析和元数据查询接口，用于前端展示
 * 
 * @author mayday
 */
@Slf4j
@RestController
@RequestMapping("/api/protocol/udp")
// @Tag(name = "UDP协议元数据", description = "UDP协议解析和元数据查询接口")
public class ProtocolMetadataController {
    
    /**
     * 解析16进制协议消息
     * 
     * @param hexString 16进制字符串
     * @return 协议消息元数据
     */
    @PostMapping("/parse")
    // @Operation(summary = "解析16进制协议消息", description = "将16进制字符串解析为协议消息实体类，并返回元数据")
    public AjaxResult parseProtocolMessage(@RequestParam String hexString) {
        try {
            UdpProtocolMessage message = UdpProtocolHexUtils.parseHexMessage(hexString);
            if (message == null) {
                return AjaxResult.error("协议解析失败：消息格式不正确");
            }
            
            ProtocolEntityMetadata metadata = ProtocolMetadataExtractor.extractMetadata(message);
            return AjaxResult.success(metadata);
        } catch (Exception e) {
            log.error("解析协议消息失败", e);
            return AjaxResult.error("解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取协议实体类的元数据定义
     * 
     * @return 协议实体元数据定义
     */
    @GetMapping("/metadata")
    // @Operation(summary = "获取协议元数据定义", description = "获取UDP协议实体类的元数据定义，用于前端展示")
    public AjaxResult getProtocolMetadata() {
        try {
            // 创建一个示例消息来提取元数据定义
            UdpProtocolMessage exampleMessage = new UdpProtocolMessage();
            UdpProtocolHeader exampleHeader = new UdpProtocolHeader();
            exampleHeader.setVersion(1);
            exampleHeader.setFrameType(UdpProtocolHeader.FrameType.DATA);
            exampleHeader.setSequence(1);
            exampleHeader.setDataLength(0);
            exampleHeader.setTimestamp(System.currentTimeMillis() / 1000);
            exampleMessage.setHeader(exampleHeader);
            
            ProtocolEntityMetadata metadata = ProtocolMetadataExtractor.extractMetadata(exampleMessage);
            return AjaxResult.success(metadata);
        } catch (Exception e) {
            log.error("获取协议元数据失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建16进制协议消息
     * 
     * @param version 版本
     * @param frameType 帧类型代码
     * @param flags 标志位
     * @param sequence 序列号
     * @param payloadHex 净荷16进制字符串
     * @return 16进制字符串
     */
    @PostMapping("/build")
    // @Operation(summary = "构建16进制协议消息", description = "根据参数构建16进制协议消息")
    public AjaxResult buildProtocolMessage(
            @RequestParam(defaultValue = "1") int version,
            @RequestParam int frameType,
            @RequestParam(defaultValue = "0") byte flags,
            @RequestParam int sequence,
            @RequestParam(required = false) String payloadHex) {
        try {
            String hexMessage = UdpProtocolHexUtils.buildHexMessage(
                    version, frameType, flags, sequence, payloadHex != null ? payloadHex : "");
            return AjaxResult.success(hexMessage);
        } catch (Exception e) {
            log.error("构建协议消息失败", e);
            return AjaxResult.error("构建失败: " + e.getMessage());
        }
    }
}



