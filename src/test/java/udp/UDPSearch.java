package udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class UDPSearch {
    public static final int LISTEN_PORT = 30000;

    public static void main(String[] args) throws Exception{
        Listener listen = listen();
        sendBroadcast();

        System.in.read();
        List<Device> devices = listen.getDeviceAndClose();
        for (Device device : devices) {
            System.out.println("Device: "+device);
        }
    }

    private static Listener listen() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT,countDownLatch);
        listener.start();
        countDownLatch.await();
        return listener;
    }

    private static void sendBroadcast() throws Exception {
        System.out.println("sendBroadcast start..");

        //搜索方，让系统自动分配端口
        DatagramSocket datagramSocket = new DatagramSocket();
        String requestData = MessageCreator.buildWithPort(LISTEN_PORT);
        byte[] data = requestData.getBytes();
        DatagramPacket send = new DatagramPacket(data, data.length);
        //目标端口,广播地址
        send.setAddress(InetAddress.getByName("255.255.255.255"));
        send.setPort(20000);
        datagramSocket.send(send);
        datagramSocket.close();

        System.out.println("sendBroadcast finish..");
    }

    private static class Device {
        int port;
        String ip;
        String sn;

        public Device(int port, String ip, String sn) {
            this.port = port;
            this.ip = ip;
            this.sn = sn;
        }

        @Override
        public String toString() {
            return "Device{" +
                    "port=" + port +
                    ", ip='" + ip + '\'' +
                    ", sn='" + sn + '\'' +
                    '}';
        }
    }

    private static class Listener extends Thread{
        private final int listenPort;
        private final CountDownLatch countDownLatch;
        private List<Device> deviceList = new ArrayList<>();
        private boolean done =false;
        private DatagramSocket ds;

        private Listener(int listenPort, CountDownLatch countDownLatch) {
            this.listenPort = listenPort;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            countDownLatch.countDown();
            try {
                ds = new DatagramSocket(listenPort);
                while (!done){
                    byte[] buffer = new byte[512];
                    DatagramPacket rece = new DatagramPacket(buffer, buffer.length);
                    ds.receive(rece);
                    String str = new String(rece.getData(), 0, rece.getLength());
                    System.out.println("受到消息：" + str);

                    String sn = MessageCreator.parseSn(str);
                    if (sn!= null){
                        //构建发送者的信息
                        Device device = new Device(rece.getPort(),rece.getAddress().getHostAddress(),sn);
                        deviceList.add(device);
                    }
                }

            }catch (Exception e){

            }finally {
                close();
            }
        }

        List<Device> getDeviceAndClose(){
            done = true;
            close();
            return deviceList;
        }

        void close(){
            if (ds != null){
                ds.close();
                ds= null;
            }
        }
    }

}
