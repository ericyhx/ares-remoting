package ares.remoting.framework.serialization;

import ares.remoting.framework.model.AresRequest;
import ares.remoting.framework.serialization.common.SerializeType;
import ares.remoting.framework.serialization.engine.SerializerEngine;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/6
 */
public class NettyDecodeHandler extends ByteToMessageDecoder {
    /**
     * 解码对象class
     */
    private Class<?> genericClz;
    /**
     * 解码对象编码所使用的的序列化类型
     */
    private SerializeType serializeType;
    public NettyDecodeHandler(Class<?> clz, SerializeType serializeType) {
        this.genericClz=clz;
        this.serializeType=serializeType;
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes()<4){
            return;
        }
        in.markReaderIndex();
        int dataLength=in.readInt();
        if(dataLength<0){
            ctx.close();
        }
        if(in.readableBytes()<dataLength){
            in.resetReaderIndex();
            return;
        }
        byte[] data=new byte[dataLength];
        in.readBytes(data);
        Object o = SerializerEngine.deSerialize(data, genericClz, serializeType.getSerializeType());
        out.add(o);
    }
}
