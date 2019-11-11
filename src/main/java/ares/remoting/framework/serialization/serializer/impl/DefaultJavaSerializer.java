package ares.remoting.framework.serialization.serializer.impl;

import ares.remoting.framework.serialization.serializer.ISerializer;

import java.io.*;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class DefaultJavaSerializer implements ISerializer {
    @Override
    public <T> byte[] serialize(T obj) {
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        ObjectOutputStream out=null;
        try {
            out=new ObjectOutputStream(bos);
            out.writeObject(obj);
            out.close();
        } catch (IOException e) {
           throw new RuntimeException(e);
        }finally {
            if(out!=null){
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bos.toByteArray();
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clz) {
        ByteArrayInputStream bis=new ByteArrayInputStream(data);
        ObjectInputStream is= null;
        T t=null;
        try {
            is = new ObjectInputStream(bis);
            t = (T) is.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
           if(is!=null){
               try {
                   is.close();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
        }
        return t;
    }
}
