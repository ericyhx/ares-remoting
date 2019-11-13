package ares.remoting.io;

import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/13
 */
public class EchoClient {
    public static void main(String[] args) {
        ByteBuffer buffer=ByteBuffer.wrap("您好，java blocking I/O".getBytes());

        CharBuffer charBuffer;
        Charset charset = Charset.defaultCharset();
        CharsetDecoder newDecoder = charset.newDecoder();
        try {
            SocketChannel socketChannel = SocketChannel.open();

            if(socketChannel.isOpen()){
                socketChannel.configureBlocking(true);
                socketChannel.setOption(StandardSocketOptions.SO_RCVBUF,128*1024);
                socketChannel.setOption(StandardSocketOptions.SO_SNDBUF,128*1024);
                socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE,true);
                socketChannel.setOption(StandardSocketOptions.SO_LINGER,5);
                socketChannel.connect(new InetSocketAddress("localhost",8085));
                if(socketChannel.isConnected()){
                    socketChannel.write(buffer);
                    ByteBuffer allocate = ByteBuffer.allocate(1024);
                    while (socketChannel.read(allocate)!=-1){
                        allocate.flip();
                        charBuffer= newDecoder.decode(allocate);
                        System.out.println(charBuffer.toString());
                        if(allocate.hasRemaining()){
                            allocate.compact();
                        }else {
                            allocate.clear();
                        }
                    }
                }else {
                    throw new RuntimeException("connect can not established");
                }
                socketChannel.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
