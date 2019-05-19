package com.csz.link.core;

import com.csz.link.impl.SocketChannelAdapter;
import com.csz.link.util.CloseUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector implements Closeable, SocketChannelAdapter.ChannalCloseCallback {
    private UUID key = UUID.randomUUID();
    private SocketChannel socketChannel;
    private ISender sender;
    private IReceiver receiver;

    public void setup(SocketChannel socketChannel) throws Exception{
        this.socketChannel = socketChannel;

        IoContext context = IoContext.getInstance();
        SocketChannelAdapter adapter = new SocketChannelAdapter(socketChannel, context.getIoProvider(), this);

        this.sender = adapter;
        this.receiver = adapter;
        readNextMessage();
    }

    private void readNextMessage() {
        if (receiver != null) {
            try {
                receiver.receiveAsync(echoReceiveListener);
            } catch (IOException e) {
                System.out.println("接受数据异常：" + e.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {
        CloseUtil.close(sender,receiver);
    }

    @Override
    public void onChannelClose(SocketChannel channel) {

    }

    private IoArgs.IoArgsEventListener echoReceiveListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStart(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            onReceNewMsg(args.bufferString());
            //读取下一条数据
            readNextMessage();
        }
    };

    protected void onReceNewMsg(String str) {
        System.out.println(key.toString() + ":" + str);
    }
}
