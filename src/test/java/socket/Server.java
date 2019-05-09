package socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws Exception{
        ServerSocket serverSocket = new ServerSocket();
        //accept的超时时间
        serverSocket.setSoTimeout(400000);
        //backlog:最大的等待连接数
        serverSocket.bind(new InetSocketAddress(2000),50);
        System.out.println("服务端开始等待链接。。");
        System.out.println("服务端信息："+serverSocket.getLocalSocketAddress() + " "+serverSocket.getLocalPort());

        for (;;) {
            Socket socket = serverSocket.accept();
            new SocketHandler(socket).start();
        }
    }

    static class SocketHandler extends Thread{
        private boolean flag = true;
        private Socket socket;

        public SocketHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                PrintStream printStream = new PrintStream(socket.getOutputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                do {
                    String line = reader.readLine();
                    if ("bye".equalsIgnoreCase(line)) {
                        flag = false;
                        printStream.println("bye");
                    }else{
                        System.out.println("rece:"+line);
                        Thread.sleep(3500);
                        printStream.println("回送："+line.length());
                    }
                }while (flag);

                printStream.close();
                reader.close();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
