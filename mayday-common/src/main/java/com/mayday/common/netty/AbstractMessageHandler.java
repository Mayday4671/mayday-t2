package com.mayday.common.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象消息处理器
 * 提供基础的消息处理功能
 * 
 * 放在common模块中，避免循环依赖
 *
 * @author mayday
 */
@Slf4j
public abstract class AbstractMessageHandler implements MessageHandler
{
    @Override
    public Object handleMessage(Object ctxObj, Object msg)
    {
        try
        {
            if (!(ctxObj instanceof ChannelHandlerContext ctx))
            {
                log.warn("Channel上下文类型不正确: {}", ctxObj != null ? ctxObj.getClass().getName() : "null");
                return null;
            }

            if (msg instanceof ByteBuf byteBuf)
            {
                return handleByteBuf(ctx, byteBuf);
            }
            else
            {
                return handleObject(ctx, msg);
            }
        }
        catch (Exception e)
        {
            log.error("处理消息时发生异常", e);
            return null;
        }
    }

    /**
     * 处理ByteBuf类型的消息
     *
     * @param ctx Channel上下文
     * @param byteBuf 消息数据
     * @return 响应消息
     */
    protected abstract Object handleByteBuf(ChannelHandlerContext ctx, ByteBuf byteBuf);

    /**
     * 处理其他类型的消息
     *
     * @param ctx Channel上下文
     * @param msg 消息对象
     * @return 响应消息
     */
    protected Object handleObject(ChannelHandlerContext ctx, Object msg)
    {
        log.warn("未实现的消息类型处理: {}", msg != null ? msg.getClass().getName() : "null");
        return null;
    }

    /**
     * 将ByteBuf转换为字符串
     *
     * @param byteBuf ByteBuf对象
     * @return 字符串
     */
    protected String byteBufToString(ByteBuf byteBuf)
    {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return new String(bytes);
    }

    /**
     * 将字符串转换为ByteBuf
     *
     * @param ctx Channel上下文
     * @param str 字符串
     * @return ByteBuf对象
     */
    protected ByteBuf stringToByteBuf(ChannelHandlerContext ctx, String str)
    {
        return ctx.alloc().buffer().writeBytes(str.getBytes());
    }
}

