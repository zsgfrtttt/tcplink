package com.csz.client;


import com.csz.client.bean.ServerInfo;
import com.csz.foo.contants.TCPContants;
import com.csz.link.util.CloseUtil;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPClient {
    private final Socket socket;
    private final ReadHandler readHandler;
    private final PrintStream printStream;

    public TCPClient(Socket socket, ReadHandler readHandler) throws Exception {
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public void exit(){
        readHandler.exit();
        CloseUtil.close(printStream,socket);
    }

    public void send(String msg){
        printStream.println(msg);
    }

    public static TCPClient startWith(ServerInfo serverInfo) throws Exception {
        Socket socket = new Socket();
        //设置读取服务器端信息超时
        socket.setSoTimeout(5000);
        socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress()), serverInfo.getPort()), 3000);
        System.out.println("已经链接服务器。");
        System.out.println("客户端信息：" + socket.getLocalAddress() + " " + socket.getLocalPort());
        System.out.println("服务端信息：" + socket.getInetAddress() + " " + socket.getPort());

        TCPClient client = null;
        try {
            ReadHandler readHandler = new ReadHandler(socket);
            readHandler.start();
            client =new TCPClient(socket,readHandler);
            return client;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("客户端连接异常："+e.getMessage());
            if (client != null){
                client.exit();
            }else{
                socket.close();
            }
        }

        return null;
    }

    static class ReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;
        private final Socket socket;

        ReadHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.inputStream = socket.getInputStream();
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
            CloseUtil.close(inputStream,socket);
        }

    }
}
