package com.csz.server;


import com.csz.link.util.CloseUtil;
import com.csz.server.handle.ClientHandler;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {

    private final int port;
    private ClientListener listener;
    private List<ClientHandler> handlers = new ArrayList<>();
    private final ExecutorService forwardingExecutorThreadPool;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public TCPServer(int portServer) {
        this.port = portServer;
        this.forwardingExecutorThreadPool = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            listener = new ClientListener();
            listener.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public synchronized void broadcast(String line) {
        for (ClientHandler handler : handlers) {
            handler.send(line);
        }
    }

    public void stop() {
        if (listener != null) {
            listener.exit();
        }
        CloseUtil.close(selector, serverSocketChannel);
        synchronized (TCPServer.this) {
            for (ClientHandler handler : handlers) {
                handler.exit();
            }
            handlers.clear();
        }
        forwardingExecutorThreadPool.shutdownNow();
    }

    final class ClientListener extends Thread implements ClientHandler.ClientHandleCallback {
        private boolean done = false;

        @Override
        public void run() {
            while (!done) {
                try {
                    //阻塞直到有响应或者被唤醒
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> keyIterator = selector.keys().iterator();
                    while (keyIterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();

                        SocketChannel clientSocketChannel = null;
                        if (key.isAcceptable()) {
                            ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();
                            //非阻塞得到连接通道
                            clientSocketChannel = socketChannel.accept();
                        }

                        try {
                            ClientHandler handler = new ClientHandler(clientSocketChannel, this);
                            synchronized (TCPServer.this) {
                                handlers.add(handler);
                            }
                            handler.readToPrint();
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("客户端链接异常：" + e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            System.out.println("服务器关闭。");
        }

        void exit() {
            done = true;
            //解除阻塞
            selector.wakeup();
        }

        @Override
        public synchronized void onSelfColsed(ClientHandler handler) {
            handlers.remove(handler);
        }

        @Override
        public void onNewMessageArrived(ClientHandler clientHandler, String msg) {
            System.out.println("tcp:服务器收到消息：-  " + clientHandler.getClientInfo() + "  :  " + msg);
            forwardingExecutorThreadPool.execute(() -> {
                synchronized (TCPServer.this) {
                    for (ClientHandler handler : handlers) {
                        if (handler == clientHandler) {
                            continue;
                        }
                        handler.send(msg);
                    }
                }
            });
        }
    }

}
