package com.csz.server;


import com.csz.foo.contants.TCPContants;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Server {
    public static void main(String[] args) throws Exception{
        TCPServer tcpServer = new TCPServer(TCPContants.PORT_SERVER);
        boolean isSuccessed = tcpServer.start();
        ServerProvider.start(TCPContants.PORT_SERVER);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        do{
            line = bufferedReader.readLine();
            tcpServer.broadcast(line);
        }while (!TCPContants.FLAG_SERVER_EXIT.equalsIgnoreCase(line));

        ServerProvider.stop();
        tcpServer.stop();
    }
}
