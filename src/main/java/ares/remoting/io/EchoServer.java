package ares.remoting.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/13
 */
public class EchoServer {
    private static ExecutorService executor= Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            if(serverSocketChannel.isOpen()){
                serverSocketChannel.configureBlocking(true);
                serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF,4*1024);
                serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR,true);
                serverSocketChannel.bind(new InetSocketAddress("localhost",8085));
                while (true){
                    try {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        executor.submit(new EchoHandler(socketChannel));
                    }catch (Exception e){

                    }
                }
            }else {
                throw new RuntimeException("server socketChannel can not open");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
