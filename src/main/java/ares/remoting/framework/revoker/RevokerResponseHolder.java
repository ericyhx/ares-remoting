package ares.remoting.framework.revoker;

import ares.remoting.framework.model.AresResponse;
import ares.remoting.framework.model.AresResponseWrapper;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @Description:储存同步调用结果，并且将结果维持在一个Map中，根据配置的服务调用超时时间判断结果是否超时。
 * @author: yuhongxi
 * @date:2019/11/8
 */
public class RevokerResponseHolder {
    /**
     * 服务返回结果Map，key是一次服务的TraceId/UUID
     */
    private static final Map<String,AresResponseWrapper> responseMap=new ConcurrentHashMap<>();
    /**
     * 单线程池，清理超时过期的返回结果
     */
    private static final ScheduledExecutorService removeExpireKeyTask=Executors.newScheduledThreadPool(1);

    static {
        removeExpireKeyTask.scheduleWithFixedDelay(()->{
            try {
                for (Map.Entry<String, AresResponseWrapper> entry : responseMap.entrySet()) {
                    boolean isExpire = entry.getValue().isExpire();
                    if(isExpire){
                        responseMap.remove(entry.getKey());
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        },60000,1000,TimeUnit.MILLISECONDS);
    }

    /**
     * tep1：（RPC调用用户线程）初始化返回结果容器,requestUniqueKey唯一标识本次调用
     * 这个方法是被`跑RPC远程调用Task`的线程在执行call()方法中代码的第一句时候初始化的，所以后续不用担心NPE。
     * @param requestUniqueKey
     */
    public static void initResponseData(String requestUniqueKey){
        responseMap.put(requestUniqueKey,AresResponseWrapper.of());
    }

    /**
     *  Step2：（RPC调用用户线程）将Netty调用异步返回结果放入阻塞队列。
     *  这个方法是被`NIO线程`运行的`pipeline`中的`handler`所调用的，将服务调用结果放入结果集Map中。
     * @param response
     */
    public static void putResultValue(AresResponse response){
        long currentTiem = System.currentTimeMillis();
        AresResponseWrapper responseWrapper = responseMap.get(response.getUniqueKey());
        if(responseWrapper==null){
            return;
        }
        responseWrapper.setResponseTime(currentTiem);
        // !!!add背后调用offer、如果队列满了直接返回false，而不是put会阻塞直到有可用空间
        responseWrapper.getResponsesQueue().add(response);
        responseMap.put(response.getUniqueKey(),responseWrapper);
    }

    /**
     * Step3：（RPC调用用户线程）从阻塞队列中获取Netty异步返回的结果值。
     * 这个方法是被`跑RPC远程调用Task`的线程调用的。
     * 特别注意：超时时间是在获取阻塞队列中结果集时候的最长等待时间。
     * @param requestUniqueKey
     * @param timeout
     * @return
     */
    public static AresResponse getValue(String requestUniqueKey,long timeout){
        AresResponseWrapper responseWrapper = responseMap.get(requestUniqueKey);
        try {
            return responseWrapper.getResponsesQueue().poll(timeout,TimeUnit.MILLISECONDS);
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            responseMap.remove(requestUniqueKey);
        }
    }
}
