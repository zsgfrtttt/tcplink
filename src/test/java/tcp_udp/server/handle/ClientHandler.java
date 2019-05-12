package tcp_udp.server.handle;

import tcp_udp.util.CloseUtil;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final Socket socket;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final CloseCallback closeCallback;

    public ClientHandler(Socket socket, CloseCallback closeCallback) throws Exception {
        this.socket = socket;
        this.closeCallback = closeCallback;
        readHandler = new ClientReadHandler(socket.getInputStream());
        writeHandler = new ClientWriteHandler(socket.getOutputStream());
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
        closeCallback.onSelfColsed(this);
    }

    public interface CloseCallback {
        void onSelfColsed(ClientHandler handler);
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
                    }else {
                        System.out.println("tcp:服务器受到消息："+line);
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
