package ares.remoting.io;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/13
 */
public class EchoHandler implements Runnable {
    private SocketChannel socketChannel;
    private ByteBuffer buffer=ByteBuffer.allocate(1024);
    public EchoHandler(SocketChannel socketChannel) {
        this.socketChannel=socketChannel;
    }

    @Override
    public void run() {
        try{
            while (socketChannel.read(buffer)!=-1){
                buffer.flip();
                socketChannel.write(buffer);
                if(buffer.hasRemaining()){
                    buffer.compact();
                }else {
                    buffer.clear();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
