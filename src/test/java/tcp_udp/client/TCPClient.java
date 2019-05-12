package tcp_udp.client;

import tcp_udp.bean.ServerInfo;
import tcp_udp.contants.TCPContants;
import tcp_udp.util.CloseUtil;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPClient {
    private static boolean CONNECTED = false;

    public static void linkWith(ServerInfo serverInfo) throws Exception {
        Socket socket = new Socket();
        //设置读取服务器端信息超时
        socket.setSoTimeout(5000);
        socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress()), serverInfo.getPort()), 3000);
        System.out.println("已经链接服务器。");
        System.out.println("客户端信息：" + socket.getLocalAddress() + " " + socket.getLocalPort());
        System.out.println("服务端信息：" + socket.getInetAddress() + " " + socket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();

            CONNECTED = true;
            write(socket);
            readHandler.exit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        socket.close();
        System.out.println("客户端exit。");
    }

    private static void write(Socket client) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        OutputStream outputStream = client.getOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        do {
            String line = bufferedReader.readLine();
            printStream.println(line);
            if (TCPContants.FLAG_CLIENG_EXIT.equalsIgnoreCase(line)) {
                break;
            }
        } while (CONNECTED);
        printStream.close();

    }

    static class ReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                do {
                    try {
                        line = reader.readLine();
                        if (line == null) {
                            System.out.println("tcp:连接服务器关闭。");
                            break;
                        } else {
                            System.out.println("tcp:客户端收到消息：" + line);
                        }
                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    System.out.println("tcp:连接服务器出现异常：" + e.getMessage());
                }
            } finally {
                exit();
            }
        }

        void exit() {
            done = true;
            CONNECTED = false;
            CloseUtil.close(inputStream);
        }
    }
}
