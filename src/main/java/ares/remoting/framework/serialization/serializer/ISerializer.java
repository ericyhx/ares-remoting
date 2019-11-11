package ares.remoting.framework.serialization.serializer;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
public interface ISerializer {
    <T> byte[] serialize(T obj);

    <T> T deSerialize(byte[] data,Class<T> clz);
}
