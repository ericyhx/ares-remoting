package ares.remoting.framework.revoker;

import ares.remoting.framework.cluster.ClusterStrategy;
import ares.remoting.framework.cluster.engine.ClusterEngine;
import ares.remoting.framework.model.AresRequest;
import ares.remoting.framework.model.AresResponse;
import ares.remoting.framework.model.ProviderService;
import ares.remoting.framework.zookeeper.RegisterCenter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/8
 */
public class RevokerProxyBeanFactory implements InvocationHandler {
    private ExecutorService fixedThreadPool=null;
    private Class<?> targetInterface;
    private int consumeTiemout;
    private static int threadWorkerNumber=10;
    private String clusterStrategy;

    private RevokerProxyBeanFactory(Class<?> targetInterface, int consumeTiemout, String clusterStrategy) {
        this.targetInterface = targetInterface;
        this.consumeTiemout = consumeTiemout;
        this.clusterStrategy = clusterStrategy;
    }
    private static volatile RevokerProxyBeanFactory singeton;
    public static RevokerProxyBeanFactory singleton(Class<?> targetInterface,int consumeTiemout,String clusterStrategy){
        if(null==singeton){
            synchronized (RevokerProxyBeanFactory.class){
                if(null==singeton){
                    singeton=new RevokerProxyBeanFactory(targetInterface,consumeTiemout,clusterStrategy);
                }
            }
        }
        return singeton;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String serviceKey = targetInterface.getName();
        //获取某个接口的服务提供者列表
        List<ProviderService> providerServices = RegisterCenter.singleton().getServiceMetaDataMap4Consume().get(serviceKey);
        ClusterStrategy clusterStrategy = ClusterEngine.queryClusterStrategy(this.clusterStrategy);
        ProviderService providerService = clusterStrategy.select(providerServices);
        ProviderService newProvider = providerService.copy();
        //设置本次调用服务的方法以及接口
        newProvider.setServiceMethod(method);
        newProvider.setServiceItf(targetInterface);

        //声明调用AresRequest对象,AresRequest表示发起一次调用所包含的信息
        AresRequest request=new AresRequest();
        //设置本次调用的唯一标识(客户端调用使用UUID也无所谓)
        request.setUniqueKey(UUID.randomUUID().toString()+"-"+Thread.currentThread().getId());
        request.setProviderService(newProvider);
        request.setInvokeTimeout(consumeTiemout);
        request.setInvokedMethodName(method.getName());
        request.setArgs(args);
        try {
            //构建用来发起调用的线程池
            if(fixedThreadPool==null){
                synchronized (RevokerProxyBeanFactory.class){
                    if(null==fixedThreadPool){
                        fixedThreadPool=Executors.newFixedThreadPool(threadWorkerNumber);
                    }
                }
            }
            String serviceIp = request.getProviderService().getServerIp();
            int serverPort = request.getProviderService().getServerPort();
            InetSocketAddress socketAddress=new InetSocketAddress(serviceIp,serverPort);
            Future<AresResponse> responseFuture = fixedThreadPool.submit(RevokerServiceCallable.of(socketAddress, request));
            AresResponse response = responseFuture.get(request.getInvokeTimeout(), TimeUnit.MILLISECONDS);
            if(response!=null){
                return response.getResult();
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return null;
    }


    public Object getProxy(){
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),new Class<?>[]{targetInterface},this);
    }

}
