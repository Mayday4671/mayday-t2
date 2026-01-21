create table `ai_key`
(
    `id`          bigint       not null AUTO_INCREMENT COMMENT '主键ID',
    `provider`    VARCHAR(32)  not null COMMENT '提供商编码：google/openai/deepseek/ollama/...（用于归类与校验）',
    `name`        VARCHAR(64)  not null COMMENT '密钥名称：便于运维识别（如 prod-key-01）',
    `key_cipher`  VARCHAR(512) not null COMMENT 'API Key 密文（建议 AES-GCM 后 base64 存储；严禁明文）',
    `status`      VARCHAR(16)  not null default 'ACTIVE' COMMENT '密钥状态：ACTIVE=可用，DISABLED=禁用，REVOKED=已撤销',
    `remark`      VARCHAR(255)          default null COMMENT '备注（例如申请人、用途、过期说明）',
    `update_time` datetime     not null default current_timestamp on update current_timestamp COMMENT '更新时间（配置变更、状态变更都会更新）',
    `create_time` datetime     not null default current_timestamp COMMENT '创建时间',
    primary key (`id`),
    key           `idx_provider_status` (`provider`, `status`) COMMENT '按提供商+状态检索密钥（路由/校验常用）'
) ENGINE=InnoDB default CHARSET=utf8mb4 COMMENT='AI密钥表（密文存储）';

create table `ai_config`
(
    `id`          bigint        not null AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   VARCHAR(64)   not null default '*' COMMENT '租户ID：* 表示全局默认；多租户系统可按租户隔离策略',
    `scene_code`  VARCHAR(64)   not null COMMENT '业务场景编码：如 article_generate/summary/qa/translate（路由入口）',

    `provider`    VARCHAR(32)   not null COMMENT '提供商编码：google/openai/deepseek/ollama/...（决定用哪个 Provider 构建模型）',
    `model_name`  VARCHAR(128)  not null COMMENT '模型名称：如 gemini-1.5-flash / gpt-4o-mini / deepseek-chat',
    `base_url`    VARCHAR(255)           default null COMMENT '自定义请求地址：用于代理/第三方OpenAI兼容/本地模型（可为空则使用默认）',

    `key_id`      bigint        not null COMMENT '关联 ai_key.id：指向该配置使用的 API Key',

    `temperature` DECIMAL(3, 2) not null default 0.70 COMMENT '采样温度：0~2（越大越发散）',
    `max_tokens`  INT                    default null COMMENT '最大输出 tokens：为空表示使用SDK默认；建议在场景级设置上限控成本',
    `timeout_ms`  INT           not null default 30000 COMMENT '请求超时毫秒：单条配置自己的超时（覆盖全局默认）',

    `priority`    INT           not null default 100 COMMENT '优先级：越小越优先（主选组）；失败后再尝试更大priority组',
    `weight`      INT           not null default 100 COMMENT '权重：同priority组内按权重选择（用于轮询/负载/多Key）',

    `enabled`     tinyint(1) not null default 1 COMMENT '是否启用：1=启用，0=停用（停用后不参与路由）',
    `version`     INT           not null default 1 COMMENT '配置版本号：变更时+1（用于缓存失效与灰度控制）',

    `remark`      VARCHAR(255)           default null COMMENT '备注：用途/说明',
    `update_time` datetime      not null default current_timestamp on update current_timestamp COMMENT '更新时间',
    `create_time` datetime      not null default current_timestamp COMMENT '创建时间',

    primary key (`id`),
    key           `idx_scene_route` (`tenant_id`, `scene_code`, `enabled`, `priority`) COMMENT '路由索引：按租户+场景取候选并按priority排序',
    key           `idx_key` (`key_id`) COMMENT '按key查询配置（排查/禁用key时用）'
) ENGINE=InnoDB default CHARSET=utf8mb4 COMMENT='AI配置表（场景路由 + 模型参数 + key绑定）';

create table `ai_call_log`
(
    `id`                bigint       not null AUTO_INCREMENT COMMENT '主键ID',
    `request_id`        VARCHAR(64)  not null COMMENT '请求ID：贯穿一次业务调用（用于串联多次回退尝试）',
    `tenant_id`         VARCHAR(64)  not null COMMENT '租户ID',
    `scene_code`        VARCHAR(64)  not null COMMENT '业务场景编码',

    `config_id`         bigint       not null COMMENT '命中的 ai_config.id（每一次尝试都记录）',
    `provider`          VARCHAR(32)  not null COMMENT '提供商编码',
    `model_name`        VARCHAR(128) not null COMMENT '模型名称',

    `success`           tinyint(1) not null COMMENT '是否成功：1成功/0失败',
    `latency_ms`        INT          not null COMMENT '耗时毫秒：从发起到返回/异常',
    `error_code`        VARCHAR(64)           default null COMMENT '错误码：建议填异常类名或供应商错误码（用于聚合统计）',
    `error_msg`         VARCHAR(255)          default null COMMENT '错误信息短句：截断存储（禁止记录密钥与敏感内容）',

    `prompt_tokens`     INT                   default null COMMENT '输入tokens（可选：部分SDK可取）',
    `completion_tokens` INT                   default null COMMENT '输出tokens（可选）',

    `create_time`       datetime     not null default current_timestamp COMMENT '创建时间（日志写入时间）',

    primary key (`id`),
    key                 `idx_req` (`request_id`) COMMENT '按request_id查看一次调用链路（含回退）',
    key                 `idx_scene_time` (`tenant_id`, `scene_code`, `create_time`) COMMENT '按租户/场景统计成功率、P95等'
) ENGINE=InnoDB default CHARSET=utf8mb4 COMMENT='AI调用日志（观测与排障）';
