package ares.remoting.framework.serialization;

import ares.remoting.framework.serialization.common.SerializeType;
import ares.remoting.framework.serialization.engine.SerializerEngine;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/6
 */
public class NettyEncodeHandler extends MessageToByteEncoder {
    private SerializeType type;
    public NettyEncodeHandler(SerializeType serializeType) {
        this.type=serializeType;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Object o, ByteBuf out) throws Exception {
        byte[] data = SerializerEngine.serialize(o, type.getSerializeType());
        out.writeInt(data.length);
        out.writeBytes(data);
    }
}
