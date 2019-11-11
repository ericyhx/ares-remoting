package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class ProtoStuffSerializer implements ISerializer {
    private static Map<Class<?>,Schema> cacheSchema=new ConcurrentHashMap<>();

    private static <T> Schema<T> getSchema(Class<T> clz){
        Schema<T> schema = (Schema<T>)cacheSchema.get(clz);
        if(schema==null){
            schema=RuntimeSchema.createFrom(clz);
            cacheSchema.put(clz,schema);
        }
        return schema;
    }
    @Override
    public <T> byte[] serialize(T obj) {
        if(obj==null){
            return new byte[0];
        }
        Class<T> clz = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(clz);
            return ProtostuffIOUtil.toByteArray(obj,schema,buffer);
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            buffer.clear();
        }
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clz) {
       try {
           T message = (T)clz.getConstructors()[0].newInstance();
           Schema<T> schema = getSchema(clz);
           ProtostuffIOUtil.mergeFrom(data,message,schema);
           return message;
       }catch (Exception e){
           throw new RuntimeException(e);
       }
    }
}
