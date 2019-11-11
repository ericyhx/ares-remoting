package ares.remoting.framework.revoker;

import ares.remoting.framework.helper.PropertyConfigHelper;
import ares.remoting.framework.model.AresResponse;
import ares.remoting.framework.model.ProviderService;
import ares.remoting.framework.serialization.NettyDecodeHandler;
import ares.remoting.framework.serialization.NettyEncodeHandler;
import ares.remoting.framework.serialization.common.SerializeType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @Description: Netty通道池工厂，队列使用‘ArrayBlockingQueue’来存放生产者
 * @author: yuhongxi
 * @date:2019/11/8
 */
@Slf4j
public class NettyChannelPoolFactory {
    private static final NettyChannelPoolFactory channelPoolFactory=new NettyChannelPoolFactory();
    /**
     * Key为服务提供者地址,value为Netty Channel阻塞队列(核心数据结构)
     */
    private static final Map<InetSocketAddress,ArrayBlockingQueue<Channel>> channelPoolMap=new ConcurrentHashMap<>();
    /**
     * 初始化Netty Channel阻塞队列的长度,该值为可配置信息
     */
    private static final int channelConnectSize=PropertyConfigHelper.getChannelConnectSize();

    private static final SerializeType serializeType=PropertyConfigHelper.getSerializeType();

    /**
     * 服务提供者列表
     */
    private List<ProviderService> serviceMetaDataList=new ArrayList<>();

    private NettyChannelPoolFactory(){}

    /**
     * 初始化Netty channel 连接队列Map
     * @param providerMap
     */
    public void initChannelPoolFactory(Map<String,List<ProviderService>> providerMap){

       if(providerMap!=null){
           //将服务提供者信息存入serviceMetaDataList列表
           for (List<ProviderService> services : providerMap.values()) {
               if(CollectionUtils.isNotEmpty(services)){
                   serviceMetaDataList.addAll(services);
               }
           }
       }
        Set<InetSocketAddress> socketAddresses=new HashSet<>();
       serviceMetaDataList.forEach(data->{
           String serviceIp = data.getServerIp();
           int serverPort = data.getServerPort();
           socketAddresses.add(new InetSocketAddress(serviceIp,serverPort));
       });
        //根据服务提供者地址列表初始化Channel阻塞队列,并以地址为Key,地址对应的Channel阻塞队列为value,存入channelPoolMap
        socketAddresses.forEach(add->{
            try{
                int realChannelConnectSize=0;
                while (realChannelConnectSize<channelConnectSize){
                    Channel channel=null;
                    while (channel==null){
                        //若channel不存在,则注册新的Netty Channel
                        channel=registerChannel(add);
                    }
                    realChannelConnectSize++;
                    //将新注册的Netty Channel存入阻塞队列channelArrayBlockingQueue
                    // 并将阻塞队列channelArrayBlockingQueue作为value存入channelPoolMap
                    ArrayBlockingQueue<Channel> channels = channelPoolMap.get(add);
                    if(channels==null){
                        channels= new ArrayBlockingQueue<>(channelConnectSize);
                        channelPoolMap.put(add,channels);
                    }
                    channels.offer(channel);
                }
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * 为服务提供者地址socketAddress注册新的Channel，
     * 如果从Zookeeper中心获取服务者列表有200个，而池子中允许的连接数最多是100个，则要初始化100个连接到服务端的netty-client通道。
     * @param socketAddress
     * @return
     */
    public Channel registerChannel(InetSocketAddress socketAddress) {
        try {
            EventLoopGroup group=new NioEventLoopGroup(10);
            Bootstrap bootstrap=new Bootstrap();
            bootstrap.remoteAddress(socketAddress);

            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new NettyEncodeHandler(serializeType));
                            ch.pipeline().addLast(new NettyDecodeHandler(AresResponse.class,serializeType));
                            ch.pipeline().addLast(new NettyClientInvokeHandler());
                        }
                    });
            // 发起客户端对服务端的连接，并同步等待
            ChannelFuture channelFuture = bootstrap.connect().sync();
            // !!!重要：Future可以返回连接通道本身
            final Channel newChannel = channelFuture.channel();


            // `isSuccessHolder`是当前线程想要等待注册结果的标记，这里用`CountDownLatch`来做同步等待连接建立
            final List<Boolean> isSuccessHolder=new ArrayList<>(1);
            final CountDownLatch connectedLatch=new CountDownLatch(1);
            // 监听Channel是否建立成功(建议使用Listener来做异步监听)
            channelFuture.addListener((ChannelFutureListener)(future)->{
                if(future.isSuccess()){
                    isSuccessHolder.add(Boolean.TRUE);
                }else {
                    future.cause().printStackTrace();
                    isSuccessHolder.add(Boolean.FALSE);
                }
                connectedLatch.countDown();
            });
            connectedLatch.await();
            if(isSuccessHolder.get(0)){
                return newChannel;
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * 根据服务提供者地址获取对应的Netty Channel阻塞队列
     * @param socketAddress
     * @return
     */
    public ArrayBlockingQueue<Channel> acquire(InetSocketAddress socketAddress){
        return channelPoolMap.get(socketAddress);
    }

    public void release(ArrayBlockingQueue<Channel> queue,Channel channel,InetSocketAddress socketAddress){
        if(queue==null){
            return;
        }
        //回收之前先检查channel是否可用,不可用的话,重新注册一个,放入阻塞队列
        if(channel==null||!channel.isActive()||!channel.isOpen()||!channel.isWritable()){
            if(channel!=null){
                channel.deregister().syncUninterruptibly().awaitUninterruptibly();
                channel.closeFuture().syncUninterruptibly().awaitUninterruptibly();
            }

            Channel newChannel=null;
            while (newChannel==null){
                log.debug("---------register new Channel-------------");
                newChannel=registerChannel(socketAddress);
            }
            queue.offer(newChannel);
            return;
        }
        queue.offer(channel);
    }
    public static NettyChannelPoolFactory channelPoolFactoryInstance(){
        return channelPoolFactory;
    }

}
