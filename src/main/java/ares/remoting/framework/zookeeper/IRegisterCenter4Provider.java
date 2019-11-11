package ares.remoting.framework.zookeeper;

import ares.remoting.framework.model.ProviderService;

import java.util.List;
import java.util.Map;

/**
 * @Description:服务端注册中心接口
 * @author: yuhongxi
 * @date:2019/11/7
 */
public interface IRegisterCenter4Provider {
    /**
     * 服务端将服务提供者信息注册到zk对应的节点下
     * @param serviceMetaData
     */
    void registerProvider(final List<ProviderService> serviceMetaData);

    /**
     * 服务端获取服务提供者信息
     * 注:返回对象,Key:服务提供者接口  value:服务提供者服务方法列表
     * @return
     */
    Map<String,List<ProviderService>> getProviderServiceMap();
}
