package ares.remoting.framework.helper;

import ares.remoting.framework.serialization.common.SerializeType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/6
 */
@Slf4j
public class PropertyConfigHelper {
    private static final String PROPERTY_CLASSPATH="/ares_remoting.properties";
    private static Properties properties;
    //ZK服务地址
    private static String zkService = "";
    //ZK session超时时间
    private static int zkSessionTimeout;
    //ZK connection超时时间
    private static int zkConnectionTimeout;
    //序列化算法类型
    private static SerializeType serializeType;
    //每个服务端提供者的Netty的连接数
    private static int channelConnectSize;
    public static String getZkService() {
        return zkService;
    }

    public static int getZkSessionTimeout() {
        return zkSessionTimeout;
    }

    public static int getZkConnectionTimeout() {
        return zkConnectionTimeout;
    }

    public static int getChannelConnectSize() {
        return channelConnectSize;
    }

    public static SerializeType getSerializeType() {
        return serializeType;
    }
    static{
        properties=new Properties();
        InputStream is=null;
        try {
            is=PropertyConfigHelper.class.getResourceAsStream(PROPERTY_CLASSPATH);
            if(null==is){
                throw new IllegalStateException("ares_remoting.properties can not found in the classpath.");
            }
            properties.load(is);
            zkService=properties.getProperty("zk_service");
            zkSessionTimeout = Integer.parseInt(properties.getProperty("zk_sessionTimeout", "500"));
            zkConnectionTimeout = Integer.parseInt(properties.getProperty("zk_connectionTimeout", "500"));
            channelConnectSize = Integer.parseInt(properties.getProperty("channel_connect_size", "10"));
            String seriType = properties.getProperty("serialize_type");
            serializeType = SerializeType.queryByType(seriType);
            if (serializeType == null) {
                serializeType=SerializeType.DefaultJavaSerializer;
            }
        }catch (Exception e){
            log.warn("load ares_remoting's properties file failed.", e);
            throw new RuntimeException(e);
        }finally {
            if(null!=is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(PropertyConfigHelper.getSerializeType());
    }
}
