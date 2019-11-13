package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 对于性能和简洁性有极高要求的场景，Hessian、protobuf、Thrift、Avro有竞争关系
 * Hessian是在性能和稳定性同时考虑下最有的序列化协议
 */
public class HessianSerializer implements ISerializer {


    @Override
    public byte[] serialize(Object obj) {
        if (obj == null)
            throw new NullPointerException();

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            HessianOutput ho = new HessianOutput(os);
            ho.writeObject(obj);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clazz) {
        if (data == null)
            throw new NullPointerException();

        try {
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            HessianInput hi = new HessianInput(is);
            return (T) hi.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}
