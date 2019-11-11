package ares.remoting.framework.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/6
 */
@Data
public class AresRequest implements Serializable {
    /**
     * UUID,唯一标识一次返回值
     */
    private String uniqueKey;
    /**
     * 服务提供者的信息
     */
    private ProviderService providerService;
    /**
     * 调用的方法名称
     */
    private String invokeMethodName;
    /**
     * 传递的参数
     */
    private Object[] args;
    /**
     * 消费端应用名
     */
    private String appName;
    //消费请求超时时长
    private long invokeTimeout;
}
