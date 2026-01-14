package com.mayday.common.netty;

/**
 * 消息处理器接口
 * UDP和TCP服务器使用此接口处理接收到的消息
 * 
 * 注意：此接口不依赖Netty具体实现，只定义方法签名
 * 具体实现类需要处理Netty的ByteBuf等类型
 *
 * @author mayday
 */
public interface MessageHandler
{
    /**
     * 处理接收到的消息
     *
     * @param ctx Channel上下文（Object类型，避免直接依赖Netty）
     * @param msg 接收到的消息（ByteBuf或其他类型）
     * @return 响应消息，如果返回null则不发送响应
     */
    Object handleMessage(Object ctx, Object msg);

    /**
     * 获取处理器名称
     *
     * @return 处理器名称
     */
    default String getName()
    {
        return this.getClass().getSimpleName();
    }
}

