package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;
import com.alibaba.fastjson.JSON;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class JSONSerializer implements ISerializer {
    @Override
    public <T> byte[] serialize(T obj) {
        if(obj==null){
            return new byte[0];
        }
        return JSON.toJSONBytes(obj);
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clz) {
        return JSON.parseObject(data,clz);
    }
}
