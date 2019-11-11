package ares.remoting.framework.serialization.engine;

import ares.remoting.framework.serialization.common.SerializeType;
import ares.remoting.framework.serialization.serializer.ISerializer;
import ares.remoting.framework.serialization.serializer.impl.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class SerializerEngine {
    private static final Map<SerializeType,ISerializer> serializerMap=new ConcurrentHashMap<>();
    static {
        serializerMap.put(SerializeType.DefaultJavaSerializer,new DefaultJavaSerializer());
        serializerMap.put(SerializeType.HessianSerializer, new HessianSerializer());
        serializerMap.put(SerializeType.JSONSerializer, new JSONSerializer());
        serializerMap.put(SerializeType.XmlSerializer, new XmlSerializer());
        serializerMap.put(SerializeType.ProtoStuffSerializer, new ProtoStuffSerializer());
        serializerMap.put(SerializeType.MarshallingSerializer, new MarshallingSerializer());

        //以下三类不能使用普通的java bean
        serializerMap.put(SerializeType.AvroSerializer, new AvroSerializer());
        serializerMap.put(SerializeType.ThriftSerializer, new ThriftSerializer());
        serializerMap.put(SerializeType.ProtocolBufferSerializer, new ProtocolBufferSerializer());
    }

    public static <T> byte[] serialize(T obj,String serializeType){
        SerializeType type = SerializeType.queryByType(serializeType);
        if(type==null){
            type=SerializeType.DefaultJavaSerializer;
        }
        ISerializer serializer = serializerMap.get(type);
        if(serializer==null){
            throw new RuntimeException("serialize error");
        }
        return serializer.serialize(obj);
    }

    public static <T> T deSerialize(byte[] data,Class<T> clz,String serializeType){
        SerializeType type = SerializeType.queryByType(serializeType);
        if(type==null){
            type=SerializeType.DefaultJavaSerializer;
        }
        ISerializer serializer = serializerMap.get(type);
        if(serializer==null){
            throw new RuntimeException("serialize error");
        }
        return serializer.deSerialize(data,clz);
    }
}
