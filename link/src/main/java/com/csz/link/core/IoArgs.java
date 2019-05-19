package com.csz.link.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IoArgs {
    private byte[] bytes = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(bytes);

    public int read(SocketChannel socketChannel) throws IOException{
        buffer.clear();
        return socketChannel.read(buffer);
    }

    public int write(SocketChannel socketChannel) throws IOException{
        //TODO clear? flip?
        return socketChannel.write(buffer);
    }

    public String bufferString(){
        //删除换行符
        return new String(bytes,0,buffer.position()-1);
    }

    public static interface IoArgsEventListener{
        void onStart(IoArgs args);

        void onCompleted(IoArgs args);
    }
}
