package ares.remoting.framework.zookeeper;

import ares.remoting.framework.model.InvokerService;
import ares.remoting.framework.model.ProviderService;

import java.util.List;
import java.util.Map;

/**
 * @Description:消费端注册中心接口
 * @author: yuhongxi
 * @date:2019/11/7
 */
public interface IRegisterCenter4Invoker {
    /**
     * 消费端初始化服务提供者信息本地缓存
     * @param remoteAppKey
     * @param groupName
     */
    void initProvideMap(String remoteAppKey,String groupName);

    /**
     * 消费端获取服务提供者信息
     * @return服务提供者
     */
    Map<String,List<ProviderService>> getServiceMetaDataMap4Consume();

    /**
     * 消费端将消费者信息注册到zk对应的节点下
     * @param invoker
     */
    void registerInvoker(final InvokerService invoker);
}
