package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;
import org.jboss.marshalling.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class MarshallingSerializer implements ISerializer {
    final static MarshallingConfiguration configuration=new MarshallingConfiguration();
    /**
     * 获取序列化工厂对象,参数serial标识创建的是java序列化工厂对象
     */
    final static MarshallerFactory factory=Marshalling.getProvidedMarshallerFactory("serial");
    static {
        configuration.setVersion(5);
    }
    @Override
    public <T> byte[] serialize(T obj) {
        final ByteArrayOutputStream bos=new ByteArrayOutputStream();
        try {
            final Marshaller marshaller=factory.createMarshaller(configuration);
            marshaller.start(Marshalling.createByteOutput(bos));
            marshaller.writeObject(obj);
            marshaller.finish();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clz) {
        try {
            ByteArrayInputStream bis=new ByteArrayInputStream(data);
            final Unmarshaller unmarshaller=factory.createUnmarshaller(configuration);
            unmarshaller.start(Marshalling.createByteInput(bis));
            T t = unmarshaller.readObject(clz);
            unmarshaller.finish();
            return t;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
