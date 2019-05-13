package com.csz.server.handle;
import com.csz.link.util.CloseUtil;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final Socket socket;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandleCallback clientHandleCallback;
    private final String clientInfo;


    public ClientHandler(Socket socket, ClientHandleCallback clientHandleCallback) throws Exception {
        this.socket = socket;
        this.clientHandleCallback = clientHandleCallback;
        this.clientInfo = "address[" + socket.getInetAddress() + "]  port[" + socket.getPort() + "].";
        readHandler = new ClientReadHandler(socket.getInputStream());
        writeHandler = new ClientWriteHandler(socket.getOutputStream());
        System.out.println("新客户端连接："+clientInfo);
    }

    public String getClientInfo(){
        return this.clientInfo;
    }

    public void send(String line) {
        writeHandler.send(line);
    }

    public void readToPrint() {
        readHandler.start();
    }

    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtil.close(socket);
    }

    public void exitBySelf() {
        exit();
        clientHandleCallback.onSelfColsed(this);
    }

    public interface ClientHandleCallback {
        void onSelfColsed(ClientHandler handler);

        void onNewMessageArrived(ClientHandler clientHandler, String msg);
    }

    class ClientReadHandler extends Thread {

        private boolean done = false;
        private final InputStream inputStream;

        ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                do {
                    line = reader.readLine();
                    if (line == null) {
                        System.out.println("tcp:连接客户端断开。");
                        done = true;
                        ClientHandler.this.exitBySelf();
                    } else {
                        clientHandleCallback.onNewMessageArrived(ClientHandler.this, line);
                        exitBySelf();
                    }
                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    e.printStackTrace();
                    ClientHandler.this.exit();
                }
            } finally {
                CloseUtil.close(inputStream);
            }
        }


        void exit() {
            done = true;
            CloseUtil.close(inputStream);
        }
    }

    class ClientWriteHandler {
        private boolean done = false;
        private final PrintStream writer;
        private final ExecutorService executorService;

        ClientWriteHandler(OutputStream outputStream) {
            this.writer = new PrintStream(outputStream);
            executorService = Executors.newSingleThreadExecutor();
        }

        public void send(String line) {
            if (done){
                return;
            }
            executorService.submit(new WriteRunnable(line));
        }

        void exit() {
            done = true;
            CloseUtil.close(writer);
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {
            private final String text;

            public WriteRunnable(String line) {
                this.text = line;
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }
                try {
                    writer.println(text);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
