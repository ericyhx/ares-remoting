package ares.remoting.framework.provider;

import ares.remoting.framework.helper.IPHelper;
import ares.remoting.framework.model.ProviderService;
import ares.remoting.framework.zookeeper.IRegisterCenter4Provider;
import ares.remoting.framework.zookeeper.RegisterCenter;
import lombok.Data;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description:服务Bean发布入口
 * @author: yuhongxi
 * @date:2019/11/7
 */
@Data
public class ProviderFactoryBean implements FactoryBean,InitializingBean {
    //服务接口
    private Class<?> serviceItf;
    //服务实现(服务实现中所有的method都会被注册到服务中心)
    private Object serviceObject;
    //服务端口
    private String serverPort;
    //服务超时时间
    private long timeout;
    //服务代理对象,暂时没有用到
    private Object serviceProxyObject;
    //服务提供者唯一标识
    private String appKey;
    //服务分组组名
    private String groupName = "default";
    //服务提供者权重,默认为1 ,范围为[1-100]
    private int weight = 1;
    //服务端线程数,默认10个线程
    private int workerThreads = 10;
    @Override
    public void afterPropertiesSet() throws Exception {
        // 这个生产者factory bean在spring中被注入依赖后，启动netty并向Zookeeper注册服务

        // Step1：启动Netty服务端监听在服务端口上
        // 其中netty中有三个处理器：`NettyDecoderHandler`、`NettyEncoderHandler`、`NettyServerInvokeHandler`
        // 前面两个处理解码和编码，最后一个处理服务端调用逻辑
        NettyServer.singleton().start(Integer.valueOf(serverPort));
        // 生成服务方发布的服务信息
        List<ProviderService> providerServiceList = buildProviderServiceInfos();
        // 初始化Zookeeper的单例包装
        IRegisterCenter4Provider registerCenter4Provider = RegisterCenter.singleton();
        // 初始化Zookeeper的链接并且将服务信息`providerServiceList`注册到ZK结点上并且监听变化
        registerCenter4Provider.registerProvider(providerServiceList);


    }
    private List<ProviderService> buildProviderServiceInfos(){
        List<ProviderService> providerList=new ArrayList<>();
        Method[] methods = serviceObject.getClass().getDeclaredMethods();
        for (Method method : methods) {
            ProviderService providerService = new ProviderService();
            providerService.setServiceItf(serviceItf);
            providerService.setServiceObject(serviceObject);
            providerService.setServerIp(IPHelper.localIp());
            providerService.setServerPort(Integer.parseInt(serverPort));
            providerService.setTimeout(timeout);
            providerService.setServiceMethod(method);
            providerService.setWeight(weight);
            providerService.setWorkerThreads(workerThreads);
            providerService.setAppKey(appKey);
            providerService.setGroupName(groupName);
            providerList.add(providerService);
        }
        return providerList;
    }

    @Override
    public Object getObject() throws Exception {
        return serviceProxyObject;
    }

    @Override
    public Class<?> getObjectType() {
        return serviceItf;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
