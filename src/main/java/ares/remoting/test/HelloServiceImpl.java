package ares.remoting.test;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/11
 */
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String value) {
        return "hello"+value+"!!!";
    }
}
