package com.mayday.common.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


/**
 * @Description: 实体类转VO通用类
 * @Author: lc
 * @Date: 2025/6/2 23:29
 * @Version: 1.0
 */
public class BeanConverterUtils
{
    /**
     * 对象列表转换：将 List<source> 转为 List<target>
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass)
    {
        if (sourceList == null || sourceList.isEmpty()) return List.of();
        List<T> targetList = new ArrayList<>();
        for (S source : sourceList)
        {
            targetList.add(convert(source, targetClass));
        }
        return targetList;
    }

    /**
     * 将 source 对象转换为目标类型对象（属性名需一致）
     */
    public static <S, T> T convert(S source, Class<T> targetClass)
    {
        if (source == null) return null;
        try
        {
            T target = targetClass.getDeclaredConstructor().newInstance();

            Field[] sourceFields = source.getClass().getDeclaredFields();
            Field[] targetFields = targetClass.getDeclaredFields();

            for (Field sourceField : sourceFields)
            {
                sourceField.setAccessible(true);
                String name = sourceField.getName();
                Object value = sourceField.get(source);

                // 找到目标类中同名字段
                for (Field targetField : targetFields)
                {
                    if (targetField.getName().equals(name))
                    {
                        targetField.setAccessible(true);
                        // 类型兼容才复制
                        if (targetField.getType().isAssignableFrom(sourceField.getType()))
                        {
                            targetField.set(target, value);
                        }
                        break;
                    }
                }
            }
            return target;
        }
        catch (Exception e)
        {
            throw new RuntimeException("实体转换失败：" + e.getMessage(), e);
        }
    }
}
