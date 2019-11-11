package ares.remoting.framework.revoker;

import ares.remoting.framework.model.AresResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Description:维持在客户端调用者机器上的、每个服务提供方各自拥有一个的netty client通道。
 * @author: yuhongxi
 * @date:2019/11/8
 */
public class NettyClientInvokeHandler extends SimpleChannelInboundHandler<AresResponse> {
    /**
     * 当一个netty-client通道可以读取时(其实就是服务调用方将结果从netty服务端通道写入并且输出时)
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AresResponse msg) throws Exception {
        //将Netty异步返回的结果存入阻塞队列,以便调用端同步获取(同步服务、而netty是NIO、异步返回的结果，因此根据配置的超时时间来判断结果是否可用)
        RevokerResponseHolder.putResultValue(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
