package ares.remoting.test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @Description:服务端发布远程服务
 * @author: yuhongxi
 * @date:2019/11/11
 */
public class MainServer {
    public static void main(String[] args) {
        final ClassPathXmlApplicationContext ctx=new ClassPathXmlApplicationContext("ares-server.xml");
        System.out.println("服务发布完成");
    }
}
