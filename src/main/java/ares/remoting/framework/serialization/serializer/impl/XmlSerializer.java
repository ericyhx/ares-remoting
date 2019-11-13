package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * 对于公司间的系统条用，性能要求在100ms以上的服务，基于xml的soap协议值得考虑
 * 对于调式环境比较恶劣的场景，JSON和XML能极大地提高调式效率
 */
public class XmlSerializer implements ISerializer {

    private static final XStream xStream = new XStream(new DomDriver());


    @Override
    public <T> byte[] serialize(T obj) {
        return xStream.toXML(obj).getBytes();
    }


    @Override
    public <T> T deSerialize(byte[] data, Class<T> clazz) {
        String xml = new String(data);
        return (T) xStream.fromXML(xml);
    }


}
