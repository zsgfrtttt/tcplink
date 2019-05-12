package com.csz.client;

import com.csz.client.bean.ServerInfo;
import com.csz.foo.contants.TCPContants;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Client {

    public static void main(String[] args) {
        ServerInfo serverInfo = ClientSearch.searchServer(10000);
        System.out.println("serverInfo :" + serverInfo);
        if (serverInfo != null){
            TCPClient client = null;
            try {
                client = TCPClient.startWith(serverInfo);
                if (client == null){
                    return;
                }
                write(client);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (client != null){
                    client.exit();
                }
            }
        }

    }

    private static void write(TCPClient client) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        do {
            String line = bufferedReader.readLine();
            client.send(line);
            if (TCPContants.FLAG_CLIENG_EXIT.equalsIgnoreCase(line)) {
                break;
            }
        } while (true);
    }
}
