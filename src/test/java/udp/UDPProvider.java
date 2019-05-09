package udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.UUID;

public class UDPProvider {


    public static void main(String[] args) throws Exception {
        String uuid = UUID.randomUUID().toString();
        Provider provider = new Provider(uuid);
        provider.start();

        System.in.read();
        provider.exit();

    }

    public static class Provider extends Thread {
        private final String sn;
        private boolean done = false;
        private DatagramSocket datagramSocket;

        Provider(String sn) {
            this.sn = sn;
        }

        @Override
        public void run() {
            try {
                System.out.println("provider start..");
                //监听20000端口
                datagramSocket = new DatagramSocket(20000);
                while (!done) {
                    byte[] buffer = new byte[512];
                    DatagramPacket rece = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(rece);
                    String str = new String(rece.getData(), 0, rece.getLength());
                    int resopnsePort = MessageCreator.parsePort(str);
                    if (resopnsePort != -1) {
                        String responseData = MessageCreator.buildWithSn(sn);
                        byte[] data = responseData.getBytes();
                        DatagramPacket send = new DatagramPacket(data, data.length);
                        send.setAddress(rece.getAddress());
                        //目标端口
                        send.setPort(resopnsePort);
                        datagramSocket.send(send);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close();
            }
            System.out.println("provider finish..");
        }

        void exit() {
            done = true;
            close();
        }

        void close() {
            if (datagramSocket != null) {
                datagramSocket.close();
                datagramSocket = null;
            }
        }
    }
}
