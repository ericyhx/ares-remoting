package ares.remoting.framework.provider;


import ares.remoting.framework.helper.PropertyConfigHelper;
import ares.remoting.framework.model.AresRequest;
import ares.remoting.framework.serialization.NettyDecodeHandler;
import ares.remoting.framework.serialization.NettyEncodeHandler;
import ares.remoting.framework.serialization.common.SerializeType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @Description:单例的Netty服务端
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class NettyServer {
    private static NettyServer server=new NettyServer();

    private Channel channel;
    /**
     * 服务端的boss线程
     */
    private EventLoopGroup bossGroup;
    /**
     * 服务端的worker线程
     */
    private EventLoopGroup workGroup;

    private SerializeType serializeType=PropertyConfigHelper.getSerializeType();

    public void start(final int port){
        synchronized (NettyServer.class){
            if(bossGroup!=null||workGroup!=null){
                return;
            }
            bossGroup=new NioEventLoopGroup();
            workGroup=new NioEventLoopGroup();
            ServerBootstrap bootstrap=new ServerBootstrap();
            bootstrap.group(bossGroup,workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new NettyDecodeHandler(AresRequest.class,serializeType));
                            ch.pipeline().addLast(new NettyEncodeHandler(serializeType));
                            ch.pipeline().addLast(new NettyServerInvokeHandler());
                        }
                    });
            try {
                channel=bootstrap.bind(port).sync().channel();
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    }
    private NettyServer(){}

    public static NettyServer singleton(){
        return server;
    }
}
