package com.mayday.auth.entity;

import com.mybatisflex.annotation.Column;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * 实体基类
 * <p>
 * 所有需要数据权限过滤的实体类都应继承此基类。
 * 该类包含一个 {@code params} 字段，用于 AOP 切面动态注入 SQL 条件。
 * </p>
 *
 * <h3>核心字段说明:</h3>
 * <ul>
 *   <li>{@code params}: 一个 Map，用于在运行时存储动态参数。
 *   数据权限切面 {@link com.mayday.auth.aspect.DataScopeAspect} 会将生成的 SQL 片段
 *   以 "dataScope" 为 key 放入此 Map 中。</li>
 * </ul>
 *
 * <h3>Mapper XML 中的使用示例:</h3>
 * <pre>{@code
 * <select id="selectUserList" resultType="SysUser">
 *     SELECT * FROM sys_user u
 *     LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
 *     WHERE u.status = '0'
 *     ${params.dataScope}  <!-- 动态注入的数据权限 SQL -->
 * </select>
 * }</pre>
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 请求参数容器
     * <p>
     * 此字段不映射到数据库表 ({@code @Column(ignore = true)})。
     * 主要用于：
     * <ol>
     *   <li>数据权限 AOP 切面注入 SQL 条件片段 (key: "dataScope")</li>
     *   <li>其他需要在运行时传递但不持久化的动态参数</li>
     * </ol>
     * </p>
     */
    @Column(ignore = true)
    private Map<String, Object> params;

    /**
     * 获取请求参数容器
     * <p>
     * 如果 {@code params} 为 null，则自动初始化为一个空的 HashMap。
     * </p>
     *
     * @return 请求参数 Map，永不为 null
     */
    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }
}
