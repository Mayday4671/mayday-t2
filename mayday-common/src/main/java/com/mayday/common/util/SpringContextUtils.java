package com.mayday.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


/**
 * @Description:
 * @Author: lc
 * @Date: 2025/7/22 20:55
 * @Version: 1.0
 */
@Component
public class SpringContextUtils implements ApplicationContextAware
{

    private static ApplicationContext context;

    public static <T> T getBean(Class<T> beanClass)
    {
        return context.getBean(beanClass);
    }

    public static Object getBean(String name)
    {
        return context.getBean(name);
    }

    public static <T> T getBean(String name, Class<T> clazz)
    {
        return context.getBean(name, clazz);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        context = applicationContext;
    }
}