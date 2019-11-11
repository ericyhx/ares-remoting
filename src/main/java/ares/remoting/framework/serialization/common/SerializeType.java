package ares.remoting.framework.serialization.common;

import lombok.Getter;

import java.util.Arrays;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/6
 */
public enum SerializeType {
    DefaultJavaSerializer("DefaultJavaSerializer"),
    HessianSerializer("HessianSerializer"),
    JSONSerializer("JSONSerializer"),
    ProtoStuffSerializer("ProtoStuffSerializer"),
    XmlSerializer("XmlSerializer"),
    MarshallingSerializer("MarshallingSerializer"),

    AvroSerializer("AvroSerializer"),
    ProtocolBufferSerializer("ProtocolBufferSerializer"),
    ThriftSerializer("ThriftSerializer");
    @Getter
    private String serializeType;
    SerializeType(String type){
        this.serializeType=type;
    }

    public static SerializeType queryByType(String type){
       return Arrays.stream(SerializeType.values()).filter(o->o.serializeType.equals(type)).findAny().orElse(null);
    }
}
