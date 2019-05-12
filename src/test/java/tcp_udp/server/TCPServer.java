package tcp_udp.server;

import tcp_udp.server.handle.ClientHandler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPServer {

    private final int port;
    private ClientListener listener;
    private List<ClientHandler> handlers = new ArrayList<>();

    public TCPServer(int portServer) {
        this.port = portServer;
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

    public void broadcast(String line) {
        for (ClientHandler handler : handlers) {
            handler.send(line);
        }
    }

    public void stop() {
        if (listener != null) {
            listener.exit();
        }
        for (ClientHandler handler : handlers) {
            handler.exit();
        }
        handlers.clear();
    }

    final class ClientListener extends Thread {
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
                    ClientHandler handler = new ClientHandler(socket, clientHandler -> {
                        handlers.remove(clientHandler);
                    });
                    handlers.add(handler);
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
    }

}
