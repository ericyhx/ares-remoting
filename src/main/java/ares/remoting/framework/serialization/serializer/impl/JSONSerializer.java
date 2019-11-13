package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @Description:
 * 基于Web Browser的Ajax以及MobileApp与服务端之间的通信JSON首选
 * 对于调式环境比较恶劣的场景，JSON和XML能极大地提高调式效率
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class JSONSerializer implements ISerializer {
    @Override
    public <T> byte[] serialize(T obj) {
        if(obj==null){
            return new byte[0];
        }
        return JSON.toJSONBytes(obj,SerializerFeature.WriteDateUseDateFormat);
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clz) {
        return JSON.parseObject(data,clz);
    }
}
