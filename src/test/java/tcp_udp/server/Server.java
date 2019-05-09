package tcp_udp.server;

import tcp_udp.contants.TCPContants;

public class Server {
    public static void main(String[] args) throws Exception{
        ServerProvider.start(TCPContants.PORT_SERVER);

        System.in.read();
        ServerProvider.stop();
    }
}
