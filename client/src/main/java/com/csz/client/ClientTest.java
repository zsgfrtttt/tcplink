package com.csz.client;

import com.csz.client.bean.ServerInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {
    private static boolean done = false;

    public static void main(String[] args) throws IOException {
        ServerInfo serverInfo = ClientSearch.searchServer(10000);
        System.out.println("serverInfo :" + serverInfo);
        if (serverInfo != null){
            List<TCPClient> tcpClientList = new ArrayList<>();
            int size = 0;
            TCPClient client;
            for (int i = 0; i < 1000; i++) {
                try {
                    client = TCPClient.startWith(serverInfo);
                    if (client == null){
                        System.out.println("连接不上服务器。。" );
                        continue;
                    }
                    tcpClientList.add(client);
                    System.out.println("当前连接数："+(++size));
                } catch (Exception e) {
                    System.out.println("连接不上服务器。。" );
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.in.read();

            Runnable writeRunnale = () -> {
                while (!done){
                    for (TCPClient tcpClient : tcpClientList) {
                        tcpClient.send("Helllo111");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread thread = new Thread(writeRunnale);
            thread.start();

            System.in.read();

            done= true;
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (TCPClient tcpClient : tcpClientList) {
                tcpClient.exit();
            }
        }
    }
}
