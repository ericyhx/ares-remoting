package ares.remoting.framework.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @Description:服务发布自定义标签
 * @author: yuhongxi
 * @date:2019/11/11
 */
public class AresRemoteReferenceNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        registerBeanDefinitionParser("reference",new RevokerFactoryBeanDefinitionParser());
    }
}
