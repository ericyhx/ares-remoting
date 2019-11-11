package ares.remoting.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * @Description:客户端调用远程服务
 * @author: yuhongxi
 * @date:2019/11/11
 */
@Slf4j
public class MainClient {
    public static void main(String[] args) throws IOException {
        //引入远程服务
        // 使用`ares-client.xml`加载spring上下文，其中定义了一个`bean id = "remoteHelloService"`
        final ClassPathXmlApplicationContext ctx=new ClassPathXmlApplicationContext("ares-client.xml");
        //获取远程服务
        // 从spring上下文中根据id取出这个bean
        HelloService helloService = (HelloService) ctx.getBean("remoteHelloService");
        Thread t1 = new Thread(() -> execute(helloService));
        t1.setName("thread-t1");
        Thread t2 = new Thread(() -> execute(helloService));
        t2.setName("thread-t2");
        t1.start();
        t2.start();


        System.in.read();

    }
    private static void execute(HelloService helloService){
        long count = 200000L;
        //调用服务并打印结果
        long start=System.currentTimeMillis();
        int i=0;
        for (i = 0; i < count; i++) {
            try {
                String result = helloService.sayHello(Thread.currentThread().getName()+"|ericYu:i=" + i);
//                System.out.println(result);
            }catch (Exception e){
                log.warn("-----",e);
            }
        }
        System.err.println(Thread.currentThread().getName()+"|cost time:"+(System.currentTimeMillis()-start)+"|count="+i);
    }
}
