package com.csz.server.handle;

import com.csz.link.core.Connector;
import com.csz.link.util.CloseUtil;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final SocketChannel socketChannel;
    private final Connector connector;
    private final ClientWriteHandler writeHandler;
    private final ClientHandleCallback clientHandleCallback;
    private final String clientInfo;


    public ClientHandler(SocketChannel socketChannel, ClientHandleCallback clientHandleCallback) throws Exception {
        this.socketChannel = socketChannel;
        this.clientHandleCallback = clientHandleCallback;
        this.clientInfo = "client[" + socketChannel.getRemoteAddress().toString() + "].";

        connector = new Connector() {
            @Override
            public void onChannelClose(SocketChannel channel) {
                super.onChannelClose(channel);
                exitBySelf();
            }

            @Override
            protected void onReceNewMsg(String str) {
                super.onReceNewMsg(str);
                clientHandleCallback.onNewMessageArrived(ClientHandler.this, str);
            }
        };
        connector.setup(socketChannel);

        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);

        writeHandler = new ClientWriteHandler(writeSelector);
        System.out.println("新客户端连接：" + clientInfo);
    }

    public String getClientInfo() {
        return this.clientInfo;
    }

    public void send(String line) {
        writeHandler.send(line);
    }

    public void exit() {
        writeHandler.exit();
        CloseUtil.close(connector);
        CloseUtil.close(socketChannel);
        System.out.println("客户端关闭：" + clientInfo);
    }

    public void exitBySelf() {
        exit();
        clientHandleCallback.onSelfColsed(this);
    }

    public interface ClientHandleCallback {
        void onSelfColsed(ClientHandler handler);

        void onNewMessageArrived(ClientHandler clientHandler, String msg);
    }

    class ClientWriteHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        public ClientWriteHandler(Selector writeSelector) {
            selector = writeSelector;
            byteBuffer = ByteBuffer.allocate(256);
            executorService = Executors.newSingleThreadExecutor();
        }

        public void send(String line) {
            if (done) {
                return;
            }
            executorService.submit(new WriteRunnable(line));
        }

        void exit() {
            done = true;
            selector.wakeup();
            CloseUtil.close(selector);
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {
            private final String text;

            public WriteRunnable(String line) {
                this.text = line + "\n";
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }
                try {
                    byteBuffer.clear();
                    byteBuffer.put(text.getBytes());
                    byteBuffer.flip();
                    while ((!done) && byteBuffer.hasRemaining()) {
                        try {
                            int len = socketChannel.write(byteBuffer);
                            if (len < 0) {
                                System.out.println("客户端无法发送数据");
                                ClientHandler.this.exit();
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
