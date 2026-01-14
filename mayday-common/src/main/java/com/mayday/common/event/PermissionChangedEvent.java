package com.mayday.common.event;

import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.List;

/**
 * 封装权限变更信息的领域事件。
 */
public class PermissionChangedEvent extends ApplicationEvent
{
    private final PermissionChangeTarget targetType;
    private final Long targetId;
    private final List<Long> affectedUserIds;
    private final String reason;

    public PermissionChangedEvent(Object source, PermissionChangeTarget targetType, Long targetId, Collection<Long> affectedUserIds, String reason)
    {
        super(source);
        this.targetType = targetType;
        this.targetId = targetId;
        this.affectedUserIds = affectedUserIds == null ? List.of() : List.copyOf(affectedUserIds);
        this.reason = reason;
    }

    public PermissionChangeTarget getTargetType()
    {
        return targetType;
    }

    public Long getTargetId()
    {
        return targetId;
    }

    public List<Long> getAffectedUserIds()
    {
        return affectedUserIds;
    }

    public String getReason()
    {
        return reason;
    }
}

