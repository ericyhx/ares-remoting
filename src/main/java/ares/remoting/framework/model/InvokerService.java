package ares.remoting.framework.model;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/6
 */
@Data
public class InvokerService implements Serializable {
    private Class<?> serviceItf;
    private Object serviceObject;
    private Method serviceMethod;
    private String invokerIp;
    private int invokerPort;
    private long timeout;
    //服务提供者的唯一表示
    private String remoteAppKey;
    //服务分组组名
    private String groupName="default";
}
