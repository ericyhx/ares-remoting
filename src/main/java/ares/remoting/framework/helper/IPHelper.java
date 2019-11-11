package ares.remoting.framework.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.*;
import java.util.Enumeration;
import java.util.List;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/6
 */
@Slf4j
public class IPHelper {
    private static String hostIp="";

    public static String localIp(){
        return hostIp;
    }
    /**
     * 获取本机Ip
     * <p/>
     * 通过 获取系统所有的networkInterface网络接口 然后遍历 每个网络下的InterfaceAddress组。
     * 获得符合 <code>InetAddress instanceof Inet4Address</code> 条件的一个IpV4地址
     *
     * @return
     */
    public static String getRealIp(){
        String localIp=null;
        String netIp=null;
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip=null;
            boolean finded=false;
            while (netInterfaces.hasMoreElements()&&!finded){
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()){
                    ip=addresses.nextElement();
                    //外网ip
                    if(!ip.isLoopbackAddress()&&!ip.isSiteLocalAddress()&&!ip.getHostAddress().contains(":")){
                        netIp=ip.getHostAddress();
                        finded=true;
                    }else
                        //内网ip
                        if(ip.isSiteLocalAddress()&&!ip.isLoopbackAddress()&&!ip.getHostAddress().contains(":")){
                        localIp=ip.getHostAddress();
                    }
                }
            }
            if(StringUtils.isNotBlank(netIp)){
                return netIp;
            }else {
                return localIp;
            }
        }catch (SocketException e){
            log.warn("获取本机Ip失败:异常信息:" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    static {
        String ip=null;
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()){
                NetworkInterface networkInterface = allNetInterfaces.nextElement();
                List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                for (InterfaceAddress address : interfaceAddresses) {
                    InetAddress Ip = address.getAddress();
                    if (Ip != null && Ip instanceof Inet4Address) {
                        if (StringUtils.equals(Ip.getHostAddress(), "127.0.0.1")) {
                            continue;
                        }
                        ip = Ip.getHostAddress();
                        break;
                    }
                }
            }
        }catch (SocketException e){
            log.warn("获取本机Ip失败:异常信息:" + e.getMessage());
            throw new RuntimeException(e);
        }
        hostIp=ip;
    }

    public static String getHostFirstIp(){
        return hostIp;
    }

    public static void main(String[] args) {
        System.out.println(getRealIp());
        System.out.println(getHostFirstIp());
    }
}
