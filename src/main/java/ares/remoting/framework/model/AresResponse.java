package ares.remoting.framework.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/6
 */
@Data
public class AresResponse implements Serializable {
    //UUID,唯一标识一次返回值
    private String uniqueKey;
    //客户端指定的服务超时时间
    private long invokeTimeout;
    //接口调用返回的结果对象
    private Object result;
}
