package ares.remoting.framework.model;

import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @Description:服务注册中心服务提供者注册信息
 * @author: yuhongxi
 * @date:2019/11/6
 */
@Data
public class ProviderService implements Serializable {
    /**
     * 服务接口的类字面变量
     */
    private Class<?> serviceItf;
    /**
     * transient关键字修饰的变量是不会被序列化的
     */
    private transient Object serviceObject;

    private transient Method serviceMethod;
    /**
     * 服务所在的ip
     */
    private String serverIp;
    /**
     * 服务所在的端口号
     */
    private int serverPort;
    /**
     * 服务调用的超时时间
     */
    private long timeout;
    /**
     * 该服务提供者的权重
     */
    private int weight;
    /**
     * 服务端的线程数
     */
    private int workerThreads;
    /**
     * 服务提供者唯一标识
     */
    private String appKey;
    /**
     * 服务分组的组名
     */
    private String groupName;

    public ProviderService copy(){
        ProviderService service=new ProviderService();
        BeanUtils.copyProperties(this,service);
        return service;
    }
}
