package com.mayday.crawler.modl.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 爬虫全局代理配置实体
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Data
@Table("crawler_proxy")
@Schema(description = "爬虫全局代理配置")
public class CrawlerProxyEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "代理名称")
    private String proxyName;

    @Schema(description = "代理类型：HTTP / SOCKS")
    private String proxyType;

    @Schema(description = "代理主机")
    private String host;

    @Schema(description = "代理端口")
    private Integer port;

    @Schema(description = "代理用户名（可选）")
    private String username;

    @Schema(description = "代理密码（可选）")
    private String password;

    @Schema(description = "是否启用：0-否 1-是")
    private Integer enabled;

    @Schema(description = "排序号（越小越靠前）")
    private Integer sort;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 请求参数容器（不映射到数据库）
     */
    @Column(ignore = true)
    private Map<String, Object> params;

    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }
}
