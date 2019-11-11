package ares.remoting.framework.model;

import lombok.Data;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @Description:
 * Netty异步调用返回结果包装类，特别注意，一次服务调用有一个确切的结果，被包装成{@link AresResponseWrapper}
 * @author: yuhongxi
 * @date:2019/11/6
 */
@Data
public class AresResponseWrapper {
    /**
     * RPC调用结果的返回时间
     */
    private long responseTime;
    /**
     * 容量只有1的阻塞队列
     */
    private BlockingQueue<AresResponse> responsesQueue=new ArrayBlockingQueue<AresResponse>(1);

    public static AresResponseWrapper of(){
        return new AresResponseWrapper();
    }

    public boolean isExpire(){
        AresResponse response = responsesQueue.peek();
        if(response==null){
            return false;
        }
        long timeout=response.getInvokeTimeout();
        if((System.currentTimeMillis()-responseTime)>timeout){
            return true;
        }
        return false;
    }

}
