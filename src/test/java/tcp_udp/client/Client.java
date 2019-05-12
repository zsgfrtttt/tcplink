package tcp_udp.client;

import tcp_udp.bean.ServerInfo;

public class Client {

    public static void main(String[] args) {
        ServerInfo serverInfo = ClientSearch.searchServer(10000);
        System.out.println("serverInfo :" + serverInfo);
        if (serverInfo != null){
            try {
                TCPClient.linkWith(serverInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
