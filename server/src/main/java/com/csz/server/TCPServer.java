package com.csz.server;


import com.csz.server.handle.ClientHandler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {

    private final int port;
    private ClientListener listener;
    private List<ClientHandler> handlers = new ArrayList<>();
    private final ExecutorService forwardingExecutorThreadPool;

    public TCPServer(int portServer) {
        this.port = portServer;
        this.forwardingExecutorThreadPool = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            listener = new ClientListener(port);
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
        synchronized (TCPServer.this) {
            for (ClientHandler handler : handlers) {
                handler.exit();
            }
            handlers.clear();
        }
        forwardingExecutorThreadPool.shutdownNow();
    }

    final class ClientListener extends Thread implements ClientHandler.ClientHandleCallback {
        private ServerSocket serverSocket;
        private boolean done = false;

        public ClientListener(int port) throws Exception {
            serverSocket = new ServerSocket(port);
        }

        @Override
        public void run() {
            while (!done) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (Exception e) {
                    continue;
                }
                try {
                    ClientHandler handler = new ClientHandler(socket,this);
                    synchronized (TCPServer.this) {
                        handlers.add(handler);
                    }
                    handler.readToPrint();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("客户端链接异常：" + e.getMessage());
                }
            }
            System.out.println("服务器关闭。");
        }

        void exit() {
            done = true;
            try {
                close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void close() throws Exception {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        }

        @Override
        public synchronized void onSelfColsed(ClientHandler handler) {
            handlers.remove(handler);
        }

        @Override
        public void onNewMessageArrived(ClientHandler clientHandler, String msg) {
            System.out.println("tcp:服务器收到消息：-  " + clientHandler.getClientInfo() + "  :  " + msg);
            forwardingExecutorThreadPool.execute(() -> {
                synchronized (TCPServer.this){
                    for (ClientHandler handler : handlers) {
                        if (handler == clientHandler){
                            continue;
                        }
                        handler.send(msg);
                    }
                }
            });
        }
    }

}
