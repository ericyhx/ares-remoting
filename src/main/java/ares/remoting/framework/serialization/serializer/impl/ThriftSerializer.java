package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

/**
 * 对于性能和简洁性有极高要求的场景，Hessian、protobuf、Thrift、Avro有竞争关系
 * Hessian是在性能和稳定性同时考虑下最有的序列化协议
 *
 * 对于需要提供一个完整的RPC解决方案，Thrift是一个好的选择
 */

public class ThriftSerializer implements ISerializer {


    @Override
    public <T> byte[] serialize(T obj) {
        try {
            TSerializer serializer = new TSerializer(new TBinaryProtocol.Factory());
            return serializer.serialize((TBase) obj);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public <T> T deSerialize(byte[] data, Class<T> clazz) {
        try {
            TBase o = (TBase) clazz.newInstance();
            TDeserializer tDeserializer = new TDeserializer();
            tDeserializer.deserialize(o, data);
            return (T) o;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}
