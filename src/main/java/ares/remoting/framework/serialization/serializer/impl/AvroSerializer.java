package ares.remoting.framework.serialization.serializer.impl;


import ares.remoting.framework.serialization.serializer.ISerializer;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 对于性能和简洁性有极高要求的场景，Hessian、protobuf、Thrift、Avro有竞争关系
 * Hessian是在性能和稳定性同时考虑下最有的序列化协议
 *
 * 但对于T级别的持久化应用场景，protobuf和Avro是首选，持久化是Hadoop子项目，Avro是更好的选择
 *
 * 对于以动态语言为主的应用场景，Avro是更好的选择
 */

public class AvroSerializer implements ISerializer {


    @Override
    public <T> byte[] serialize(T obj) {
        try {
            DatumWriter userDatumWriter = new SpecificDatumWriter(obj.getClass());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BinaryEncoder binaryEncoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            userDatumWriter.write(obj, binaryEncoder);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public <T> T deSerialize(byte[] data, Class<T> clazz) {
        try {
            DatumReader userDatumReader = new SpecificDatumReader(clazz);
            BinaryDecoder binaryDecoder = DecoderFactory.get().directBinaryDecoder(new ByteArrayInputStream(data), null);
            return (T) userDatumReader.read(clazz.newInstance(), binaryDecoder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}
