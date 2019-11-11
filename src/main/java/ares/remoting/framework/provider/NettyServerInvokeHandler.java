package ares.remoting.framework.provider;

import ares.remoting.framework.model.AresRequest;
import ares.remoting.framework.model.AresResponse;
import ares.remoting.framework.model.ProviderService;
import ares.remoting.framework.zookeeper.IRegisterCenter4Provider;
import ares.remoting.framework.zookeeper.RegisterCenter;
import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
@Slf4j
@ChannelHandler.Sharable
public class NettyServerInvokeHandler extends SimpleChannelInboundHandler<AresRequest> {
    /**
     * 用于服务端限流
     */
    private static final Map<String,Semaphore> serviceKeySemaphoreMap=new ConcurrentHashMap<>();
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AresRequest request) throws Exception {
        if(ctx.channel().isWritable()){
            ProviderService metaDataModel = request.getProviderService();
            long consumeTimeout = request.getInvokeTimeout();
            final String methodName = request.getInvokedMethodName();
            //获取限流工具类
            String serviceKey = metaDataModel.getServiceItf().getName();
            int workerThreads = metaDataModel.getWorkerThreads();
            Semaphore semaphore = serviceKeySemaphoreMap.get(serviceKey);
            if(semaphore==null){
                synchronized (serviceKeySemaphoreMap){
                    semaphore=serviceKeySemaphoreMap.get(serviceKey);
                    if(semaphore==null){
                        semaphore=new Semaphore(workerThreads);
                        serviceKeySemaphoreMap.put(serviceKey,semaphore);
                    }
                }
            }
            /**
             * !!!特别注意：
             * 一次远程服务调用重要步骤：
             * 当服务生产者机器在部署应用的时候，spring扫描xml生成bean工厂时，会解析自定义的服务信息（接口、实现类、集群地址等）。
             * 接着这个bean被注册到context环境中、第一次从bean工厂doGetBean依赖注入时候，这个bean会向Zookeeper集群注册自身的服务信息。
             * 此时Zookeeper的某个临时结点中就有了这个生产者发布的服务信息。
             * 而后这个服务生产者实例化了Netty服务端，并将它绑定在本机的IP地址和端口号上持续监听。
             * 每当有某个客户端通过netty的client、使用IP地址+端口号调用到这个生产者的netty服务端时——
             * 服务生产者的netty服务端就会先解码、获取客户端想要调用的服务、使用反射去执行目标方法、将产生的结果写入通道中，客户端就会收到服务端执行的结果。
             */
            // 获取注册中心服务：从单例Zookeeper包装类`RegisterCenter`注册中心中根据`serviceKey`拿到这个服务方提供的所有方法
            IRegisterCenter4Provider registerCenter4Provider = RegisterCenter.singleton();
            List<ProviderService> localProviderCaches = registerCenter4Provider.getProviderServiceMap().get(serviceKey);
            Object result=null;
            // 限流令牌，true代表获得
            boolean acquire=false;
            try {
                ProviderService localProviderCache = localProviderCaches.stream().filter(o -> StringUtils.equals(o.getServiceMethod().getName(), methodName)).findFirst().orElse(null);
                if(localProviderCache!=null){
                    Object serviceObject = localProviderCache.getServiceObject();
                    // !!!最重要的核心是在服务提供方这里使用反射调用服务。
                    //利用反射发起服务调用
                    Method method = localProviderCache.getServiceMethod();
                    acquire=semaphore.tryAcquire(consumeTimeout,TimeUnit.MILLISECONDS);
                    if(acquire){
                        result=method.invoke(serviceObject,request.getArgs());
                    }
                }
            }catch (Exception e){
                log.warn(JSON.toJSONString(localProviderCaches) + "  " + methodName+" "+e.getMessage());
                result=e;
            }finally {
                if(acquire){
                    semaphore.release();
                }
            }
            AresResponse response=new AresResponse();
            response.setInvokeTimeout(consumeTimeout);
            response.setUniqueKey(request.getUniqueKey());
            response.setResult(result);
            // 将服务调用返回对象回写到消费端(使用netty上下文写入通道中并且flush出去)
            ctx.channel().writeAndFlush(response);
        }else {
            log.error("------------channel closed!---------------");
        }
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
