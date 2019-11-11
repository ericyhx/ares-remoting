package ares.remoting.framework.zookeeper;

import ares.remoting.framework.helper.IPHelper;
import ares.remoting.framework.helper.PropertyConfigHelper;
import ares.remoting.framework.model.InvokerService;
import ares.remoting.framework.model.ProviderService;
import com.alibaba.fastjson.JSON;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mortbay.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @Description:注册中心实现
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class RegisterCenter implements IRegisterCenter4Governance, IRegisterCenter4Provider, IRegisterCenter4Invoker {
    private static RegisterCenter registerCenter = new RegisterCenter();
    /**
     * 服务提供者列表,Key:服务提供者接口  value:服务提供者服务方法列表
     * 存放服务+服务提供者原信息，可能被多个服务者共同注册
     */
    private static final Map<String, List<ProviderService>> providerServiceMap = new ConcurrentHashMap<>();
    /**
     * 服务端ZK服务元信息,选择服务(第一次直接从ZK拉取,后续由ZK的监听机制主动更新)
     */
    private static final Map<String, List<ProviderService>> serviceMetaDataMap4Consume = new ConcurrentHashMap<>();
    /**
     * 以下内容是Zookeeper根据properties配置文件生成的
     */
    private static String ZK_SERVICE = PropertyConfigHelper.getZkService();
    private static int ZK_SESSION_TIME_OUT = PropertyConfigHelper.getZkConnectionTimeout();
    private static int ZK_CONNECTION_TIME_OUT = PropertyConfigHelper.getZkConnectionTimeout();
    private static String ROOT_PATH = "/config_register";
    public static String PROVIDER_TYPE = "provider";
    public static String INVOKER_TYPE = "consumer";
    /**
     * 具体Zookeeper操作的客户端
     */
    private static volatile ZkClient zkClient = null;

    private RegisterCenter() {
    }

    public static RegisterCenter singleton() {
        return registerCenter;
    }

    @Override
    public Pair<List<ProviderService>, List<InvokerService>> queryProvidersAndInvokers(String serviceName, String appKey) {
        //服务消费者列表
        List<InvokerService> invokerServices = new ArrayList<>();
        //服务提供者列表
        List<ProviderService> providerServices = new ArrayList<>();
        //连接zk
        if (zkClient == null) {
            synchronized (RegisterCenter.class) {
                if (zkClient == null) {
                    zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT, ZK_CONNECTION_TIME_OUT, new SerializableSerializer());
                }
            }
        }

        String parentPath = ROOT_PATH + "/" + appKey;
        //获取 ROOT_PATH + APP_KEY注册中心子目录列表
        List<String> groupServiceList = zkClient.getChildren(parentPath);
        if (CollectionUtils.isEmpty(groupServiceList)) {
            return Pair.of(providerServices, invokerServices);
        }

        for (String group : groupServiceList) {
            String groupPath = parentPath + "/" + group;
            //获取ROOT_PATH + APP_KEY + group 注册中心子目录列表
            List<String> serviceList = zkClient.getChildren(groupPath);
            if (CollectionUtils.isEmpty(serviceList)) {
                continue;
            }
            for (String service : serviceList) {
                //获取ROOT_PATH + APP_KEY + group +service 注册中心子目录列表
                String servicePath = groupPath + "/" + service;
                List<String> serviceTypes = zkClient.getChildren(servicePath);
                if (CollectionUtils.isEmpty(serviceTypes)) {
                    continue;
                }
                for (String serviceType : serviceTypes) {
                    if (StringUtils.equals(serviceType, PROVIDER_TYPE)) {
                        //获取ROOT_PATH + APP_KEY + group +service+serviceType 注册中心子目录列表
                        String providerPath = servicePath + "/" + serviceType;
                        List<String> providers = zkClient.getChildren(providerPath);
                        if (CollectionUtils.isEmpty(providers)) {
                            continue;
                        }

                        //获取服务提供者信息
                        for (String provider : providers) {
                            String[] providerNodeArr = StringUtils.split(provider, "|");

                            ProviderService providerService = new ProviderService();
                            providerService.setAppKey(appKey);
                            providerService.setGroupName(group);
                            providerService.setServerIp(providerNodeArr[0]);
                            providerService.setServerPort(Integer.parseInt(providerNodeArr[1]));
                            providerService.setWeight(Integer.parseInt(providerNodeArr[2]));
                            providerService.setWorkerThreads(Integer.parseInt(providerNodeArr[3]));
                            providerServices.add(providerService);

                        }

                    } else if (StringUtils.equals(serviceType, INVOKER_TYPE)) {
                        //获取ROOT_PATH + APP_KEY + group +service+serviceType 注册中心子目录列表
                        String invokerPath = servicePath + "/" + serviceType;
                        List<String> invokers = zkClient.getChildren(invokerPath);
                        if (CollectionUtils.isEmpty(invokers)) {
                            continue;
                        }

                        //获取服务消费者信息
                        for (String invoker : invokers) {
                            InvokerService invokerService = new InvokerService();
                            invokerService.setRemoteAppKey(appKey);
                            invokerService.setGroupName(group);
                            invokerService.setInvokerIp(invoker);
                            invokerServices.add(invokerService);
                        }
                    }
                }
            }

        }
        return Pair.of(providerServices, invokerServices);
    }

    /**
     * 在服务调用方spring的bean工厂中代理生成的时候调用，将Zookeeper中已注册的服务信息放入到`serviceMetaDataMap4Consume`中。
     * PS:服务调用者触发是否有点不合理？理论上应该由服务中心自己维护与刷新。
     * @param remoteAppKey
     * @param groupName
     */
    @Override
    public void initProvideMap(String remoteAppKey, String groupName) {
        if(MapUtils.isEmpty(serviceMetaDataMap4Consume)){
            serviceMetaDataMap4Consume.putAll(fetchOrUpdateServiceData(remoteAppKey,groupName));
        }
    }

    /**
     * 服务调用消费方从服务中心获取已经存在的，或者更新监听变更后的
     * @param remoteAppKey
     * @param groupName
     * @return
     */
    private Map<String, List<ProviderService>> fetchOrUpdateServiceData(String remoteAppKey, String groupName) {
        Map<String, List<ProviderService>> providerServiceMap=new ConcurrentHashMap<>();
        synchronized (RegisterCenter.class){
            if(zkClient==null){
                zkClient=new ZkClient(ZK_SERVICE,ZK_SESSION_TIME_OUT,ZK_CONNECTION_TIME_OUT,new SerializableSerializer());
            }
            //从ZK获取服务提供者列表：
            // 服务注册结点(根路径+app应用名+分组名)，获取节点下的所有列表(children)
            // 第一层路径是：根路径+应用名+分组名
            String providePath = ROOT_PATH + "/" + remoteAppKey + "/" + groupName;
            List<String> providerServices = zkClient.getChildren(providePath);
            for (String serviceName : providerServices) {
                // 第二层路径是：第一层路径+服务名+服务提供者标识
                String servicePath = providePath + "/" + serviceName + "/" + PROVIDER_TYPE;
                List<String> ipPathList = zkClient.getChildren(servicePath);

                for (String ipPath : ipPathList) {
                    // 路径信息格式：IP地址、端口号、权重、工作线程数、分组
                    String serverIp = StringUtils.split(ipPath, "|")[0];
                    String serverPort = StringUtils.split(ipPath, "|")[1];
                    int weight = Integer.parseInt(StringUtils.split(ipPath, "|")[2]);
                    int workerThreads = Integer.parseInt(StringUtils.split(ipPath, "|")[3]);
                    String group = StringUtils.split(ipPath, "|")[4];
                    List<ProviderService> providerServicesList = providerServiceMap.get(serviceName);
                    if(providerServicesList==null){
                        providerServicesList=new ArrayList<>();
                    }
                    // 服务调用者信息
                    ProviderService providerService = new ProviderService();
                    // 设置服务接口
                    try {
                        providerService.setServiceItf(ClassUtils.getClass(serviceName));
                    }catch (ClassNotFoundException e){
                        throw new RuntimeException(e);
                    }
                    // 设置服务属性
                    providerService.setServerIp(serverIp);
                    providerService.setServerPort(Integer.parseInt(serverPort));
                    providerService.setWeight(weight);
                    providerService.setWorkerThreads(workerThreads);
                    providerService.setGroupName(group);
                    providerServicesList.add(providerService);

                    // 按照服务名放入服务提供者列表
                    providerServiceMap.put(serviceName,providerServicesList);
                }
                //监听注册服务的变化,同时更新数据到本地缓存
                zkClient.subscribeChildChanges(servicePath, new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                        if(currentChilds==null){
                            currentChilds=new ArrayList<>();
                        }
                        List<String> collect = currentChilds.stream().map(s -> StringUtils.split(s, "|")[0]).collect(Collectors.toList());
                        refreshServiceMetaDataMap(collect);
                    }
                });
            }
        }
        return providerServiceMap;
    }

    private void refreshServiceMetaDataMap(List<String> serviceIpList) {
        // 最新服务提供列表
        Map<String, List<ProviderService>> currentServiceMetaDataMap = new HashMap<>();

        // 遍历已经存在的服务
        for (Map.Entry<String, List<ProviderService>> entry : serviceMetaDataMap4Consume.entrySet()) {

            // 服务接口名
            String serviceItfKey = entry.getKey();
            // 服务提供列表
            List<ProviderService> serviceList = entry.getValue();

            // 如果第一次遍历到、则新建服务List放入，后续遍历到则拿出存在的List列表
            List<ProviderService> providerServiceList = currentServiceMetaDataMap.get(serviceItfKey);
            if (providerServiceList == null) {
                providerServiceList = new ArrayList<>();
            }

            // 如果原来的服务现在还在最新的IP列表里，则放入服务List中，否则抛弃
            for (ProviderService serviceMetaData : serviceList) {
                if (serviceIpList.contains(serviceMetaData.getServerIp())) {
                    providerServiceList.add(serviceMetaData);
                }
            }

            // 刷新这个接口名下的服务提供列表
            currentServiceMetaDataMap.put(serviceItfKey, providerServiceList);
        }

        // 清空服务提供Map，并且重新放入
        serviceMetaDataMap4Consume.clear();
        serviceMetaDataMap4Consume.putAll(currentServiceMetaDataMap);
    }

    @Override
    public Map<String, List<ProviderService>> getServiceMetaDataMap4Consume() {
        return serviceMetaDataMap4Consume;
    }

    @Override
    public void registerInvoker(InvokerService invoker) {
        if (invoker == null) {
            return;
        }

        //连接zk,注册服务
        synchronized (RegisterCenter.class) {

            if (zkClient == null) {
                zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT, ZK_CONNECTION_TIME_OUT, new SerializableSerializer());
            }
            //创建 ZK命名空间/当前部署应用APP命名空间/
            boolean exist = zkClient.exists(ROOT_PATH);
            if (!exist) {
                zkClient.createPersistent(ROOT_PATH, true);
            }

            //创建服务消费者节点
            String remoteAppKey = invoker.getRemoteAppKey();
            String groupName = invoker.getGroupName();
            String serviceNode = invoker.getServiceItf().getName();
            String servicePath = ROOT_PATH + "/" + remoteAppKey + "/" + groupName + "/" + serviceNode + "/" + INVOKER_TYPE;
            exist = zkClient.exists(servicePath);
            if (!exist) {
                zkClient.createPersistent(servicePath, true);
            }

            //创建当前服务器节点
            String localIp = IPHelper.localIp();
            String currentServiceIpNode = servicePath + "/" + localIp;
            exist = zkClient.exists(currentServiceIpNode);
            if (!exist) {
                //注意,这里创建的是临时节点
                zkClient.createEphemeral(currentServiceIpNode);
            }
        }
    }

    /**
     * 注册生产者是被具体某个服务提供方在spring初始化的时候调用的
     * @param serviceMetaData
     */
    @Override
    public void registerProvider(List<ProviderService> serviceMetaData) {
        if (CollectionUtils.isEmpty(serviceMetaData)) {
            return;
        }

        //连接zk,注册服务
        /**
         * 特别注意，这个`synchronized`有三重作用：
         * 1、保证`Zookeeper`只被实例化一次；
         * 2、保证`providerServiceMap`是被互斥访问的；
         * 3、看似保证`Zookeeper`上服务路径结点只有集群中第一台机器来创建（Zookeeper有分布式锁，即便同时来注册，也只有一台机器能首先注册成功，其余均失败）
         */
        synchronized (RegisterCenter.class){
            // 遍历这个服务提供方提供的所有服务
            for (ProviderService provider : serviceMetaData) {
                String serviceItfKey = provider.getServiceItf().getName();
                List<ProviderService> providers = providerServiceMap.get(serviceItfKey);
                if(providers==null){
                    providers=new ArrayList<>();
                }
                providers.add(provider);
                providerServiceMap.put(serviceItfKey,providers);
            }
            if(zkClient==null){
                zkClient=new ZkClient(ZK_SERVICE,ZK_SESSION_TIME_OUT,ZK_CONNECTION_TIME_OUT,new SerializableSerializer());
            }
            //创建 ZK命名空间/当前部署应用APP命名空间/
            String APP_KEY = serviceMetaData.get(0).getAppKey();
            String ZK_PATH = ROOT_PATH.concat("/").concat(APP_KEY);
            // 路径`Zookeeper根路径+应用名`是集群中第一台机器来注册吗？
            boolean exist = zkClient.exists(ZK_PATH);
            if(!exist){
                // 是集群中第一台服务提供者机器，就创建一个永久应用名结点
                zkClient.createPersistent(ZK_PATH,true);
            }
            // 循环遍历最新的服务Map
            for (Map.Entry<String, List<ProviderService>> entry : providerServiceMap.entrySet()) {

                //创建服务提供者
                //服务分组
                String groupName = entry.getValue().get(0).getGroupName();
                //服务接口名
                String serviceNode = entry.getKey();
                // Zookeeper中全路径：`Zookeeper根路径`+分组名+服务名+`消费者/生产者`
                String servicePath=ZK_PATH+"/"+groupName+"/"+serviceNode+"/"+PROVIDER_TYPE;
                exist=zkClient.exists(servicePath);
                if(!exist){
                    // 是集群中第一台注册这个分组的这个服务的，就创建永久服务结点
                    zkClient.createPersistent(servicePath,true);
                }

                //创建当前服务器节点
                int serverPort = entry.getValue().get(0).getServerPort();
                int weight = entry.getValue().get(0).getWeight();
                int workerThreads = entry.getValue().get(0).getWorkerThreads();
                String ip = IPHelper.localIp();
                /**
                 * 服务IP路径：
                 * `servicePath`是：`Zookeeper根路径`+分组名+服务名+`消费者/生产者`；
                 * `currentServiceIpNode`是`servicePath`+`Zookeeper所在IP地址`+`服务端口号`+`服务权重`+`工作线程`+`服务分组名`
                 */
                String currentServiceIpNode=servicePath+"/"+ip+"|"+serverPort+"|"+weight+"|"+workerThreads+"|"+groupName;
                exist=zkClient.exists(currentServiceIpNode);
                if(!exist){
                    //注意，这里创建的是临时节点
                    zkClient.createEphemeral(currentServiceIpNode);
                }
                //监听注册服务的变化，同事更新数据到本地缓存
                // 在这个服务节点上注册监听器(不同服务提供方可能会引起`服务名+分组名`结点的变化)
                zkClient.subscribeChildChanges(servicePath, new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                        if(currentChilds==null){
                            currentChilds=new ArrayList<>();
                        }
                        //存活的服务Ip列表
                        List<String> activityServiceIpList = currentChilds.stream().map(input -> StringUtils.split(input, "|")[0]).collect(Collectors.toList());
                        refreshActivityService(activityServiceIpList);
                    }
                });
            }
        }
    }

    /**
     * 利用ZK的自动刷新机制监听服务名分组名中存活服务提供者列表数据。
     * @param activityServiceIpList 异步监听传入的变化
     */
    private void refreshActivityService(List<String> activityServiceIpList) {
        Map<String,List<ProviderService>> currentServiceMetaDataMap=new HashMap<>();
        providerServiceMap.entrySet().stream().forEach(entry->{
            String key = entry.getKey();
            List<ProviderService> providers = entry.getValue();
            List<ProviderService> providerServices = currentServiceMetaDataMap.get(key);
            if(providerServices==null){
                providerServices=new ArrayList<>();
            }
            for (ProviderService provider : providers) {
                if(activityServiceIpList.contains(provider.getServerIp())){
                    providerServices.add(provider);
                }
            }
            currentServiceMetaDataMap.put(key,providerServices);
        });
        providerServiceMap.clear();
        Log.info("currentServiceMetaDataMap:{}",JSON.toJSONString(currentServiceMetaDataMap));
        providerServiceMap.putAll(currentServiceMetaDataMap);
    }

    @Override
    public Map<String, List<ProviderService>> getProviderServiceMap() {
        return providerServiceMap;
    }
}
