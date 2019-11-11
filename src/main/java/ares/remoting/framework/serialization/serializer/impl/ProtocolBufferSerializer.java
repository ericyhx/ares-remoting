package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;
import org.apache.commons.lang3.reflect.MethodUtils;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class ProtocolBufferSerializer implements ISerializer {
    @Override
    public <T> byte[] serialize(T obj) {
        try {
            return (byte[]) MethodUtils.invokeMethod(obj,"toByteArray");
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clz) {
        try {
            Object o = MethodUtils.invokeStaticMethod(clz, "getDefaultInstance");
            return (T) MethodUtils.invokeMethod(o,"parseFrom",new Object[]{data});
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
