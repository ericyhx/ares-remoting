package ares.remoting.framework.revoker;

import ares.remoting.framework.model.InvokerService;
import ares.remoting.framework.model.ProviderService;
import ares.remoting.framework.zookeeper.IRegisterCenter4Invoker;
import ares.remoting.framework.zookeeper.RegisterCenter;
import lombok.Data;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;

/**
 * @Description:服务bean引入入口。
 * @author: yuhongxi
 * @date:2019/11/8
 */
@Data
public class RevokerFactoryBean implements FactoryBean,InitializingBean {

    private Class<?> targetInterface;

    private int timeout;
    /**
     * 服务bean（是一个jdk的动态代理）
     */
    private Object serviceObject;

    private String clusterStrategy;

    private String remoteAppKey;

    private String groupName="default";

    @Override
    public Object getObject() throws Exception {
        return serviceObject;
    }

    @Override
    public Class<?> getObjectType() {
        return targetInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     *
     * !!!特别注意：
     * 一次客户端启动和调用服务的过程：
     * 每当一个应用使用标签<xxx框架:reference interface="yyy" .../>在xml中定义了一个服务引用，
     * spring使用`xxxXmlBeanDefinitionReader`生成bean工厂时(xxx代表自定义的reader，如引入velocity引擎先渲染解析一把语法)，
     * 就会向context上下文中注册一个bean，这个bean等同于引入服务的代理。
     * 它先从ZooKeeper中拉取所有相关应用、相关服务和分组的服务提供方(可缓存到本地持续监听服务上下线)，
     * 而后在本应用的机器上实例化一个Netty客户端池子，为每个服务提供方建立一个netty-client通道，维持一份ip地址到通道的映射。
     * 然后为目标类的目标接口方法(服务)建立JDK动态代理，真实调用的时候会从集群中根据某种算法挑选出一个具体的服务提供方。
     * 本机客户端调用方法的时候，就会被代理拦截，而真正的从集群中选取一个服务提供方、使用netty通道去通信。
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
//获取服务注册中心
        IRegisterCenter4Invoker registerCenter4Consumer = RegisterCenter.singleton();
        //初始化服务提供者列表到本地缓存
        registerCenter4Consumer.initProvideMap(remoteAppKey, groupName);

        //初始化Netty Channel(从Zookeeper配置中心拉取到多少服务，就建立多少netty-client，维持在ip地址对应ArrayBlockingQueue<Channel>的Map中)
        // `providerMap`是从Zookeeper中拉取的
        Map<String, List<ProviderService>> providerMap = registerCenter4Consumer.getServiceMetaDataMap4Consume();
        if (MapUtils.isEmpty(providerMap)) {
            throw new RuntimeException("service provider list is empty.");
        }
        NettyChannelPoolFactory.channelPoolFactoryInstance().initChannelPoolFactory(providerMap);

        //获取服务提供者代理对象(是JDK的动态代理)
        RevokerProxyBeanFactory proxyFactory = RevokerProxyBeanFactory.singleton(targetInterface, timeout, clusterStrategy);
        this.serviceObject = proxyFactory.getProxy();

        //将消费者信息注册到注册中心(让注册中心知道有多少服务消费者)
        InvokerService invoker = new InvokerService();
        invoker.setServiceItf(targetInterface);
        invoker.setRemoteAppKey(remoteAppKey);
        invoker.setGroupName(groupName);

        // 将消费者信息注册到注册中心
        registerCenter4Consumer.registerInvoker(invoker);
    }
}
