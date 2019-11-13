package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;
import org.apache.commons.lang3.reflect.MethodUtils;

/**
 * @Description:
 * 对于性能和简洁性有极高要求的场景，Hessian、protobuf、Thrift、Avro有竞争关系
 * Hessian是在性能和稳定性同时考虑下最有的序列化协议
 * 但对于T级别的持久化应用场景，protobuf和Avro是首选，持久化是Hadoop子项目，Avro是更好的选择
 *
 * 对于持久层非Hadoop的项目，一静态类型语言为主的应用场景，protobuf更符合
 *
 * 支持不用的传输层协议，或者需要跨防火墙访问的高性能场景，protobuf可以优先考虑
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
