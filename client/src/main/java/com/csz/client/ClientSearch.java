package com.csz.client;


import com.csz.client.bean.ServerInfo;
import com.csz.foo.contants.UDPContants;
import com.csz.link.util.ByteUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientSearch {
    private static final int LSITEN_PORT = UDPContants.PORT_CLIENT_RESPONSE;

    public static ServerInfo searchServer(int timeout) {
        try {
            CountDownLatch receiveLatch = new CountDownLatch(1);
            Listener listener = listen(receiveLatch);
            sendBroadcast();
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
            if (listener == null) return null;
            List<ServerInfo> serverInfos = listener.getServerAndClose();
            if (!serverInfos.isEmpty()) return serverInfos.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void sendBroadcast() throws Exception {
        System.out.println("sendBroadcast start..");

        //搜索方，让系统自动分配端口
        DatagramSocket datagramSocket = new DatagramSocket();

        ByteBuffer buffer = ByteBuffer.allocate(128);
        buffer.put(UDPContants.HEADER);
        buffer.putShort((short) 1);
        buffer.putInt(LSITEN_PORT);
        DatagramPacket send = new DatagramPacket(buffer.array(),buffer.position());
        //目标端口,广播地址
        send.setAddress(InetAddress.getByName("255.255.255.255"));
        send.setPort(UDPContants.PORT_SERVER);
        datagramSocket.send(send);
        datagramSocket.close();

        System.out.println("sendBroadcast finish..");
    }

    private static Listener listen(CountDownLatch receiveLatch) throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        Listener listener = new Listener(LSITEN_PORT, receiveLatch, startLatch);
        listener.start();//start()是异步启动线程，同步器让线程开启完成
        startLatch.await();
        return listener;
    }

    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch receLatch;
        private final CountDownLatch startLatch;
        private final byte[] buffer = new byte[128];
        private final int minLen = UDPContants.HEADER.length + 2 + 4;
        private List<ServerInfo> serverInfos = new ArrayList<>();
        private boolean done = false;
        private DatagramSocket ds;

        private Listener(int listenPort, CountDownLatch receLatch, CountDownLatch startLatch) {
            this.listenPort = listenPort;
            this.receLatch = receLatch;
            this.startLatch = startLatch;
        }

        @Override
        public void run() {
            startLatch.countDown();
            try {
                ds = new DatagramSocket(listenPort);
                DatagramPacket rece = new DatagramPacket(buffer, buffer.length);

                while (!done) {
                    ds.receive(rece);

                    int length = rece.getLength();
                    byte[] data = rece.getData();
                    String ip = rece.getAddress().getHostAddress();
                    boolean isVaild = length >= minLen && ByteUtil.startWith(data, UDPContants.HEADER);
                    if (!isVaild) {
                        continue;
                    }
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer,UDPContants.HEADER.length,length);
                    short cmd = byteBuffer.getShort();
                    int port = byteBuffer.getInt();
                    if (cmd != 2 || port <= 0){
                        continue;
                    }
                    String sn = new String(buffer,minLen,length - minLen);
                    ServerInfo serverInfo = new ServerInfo(sn,port,ip);
                    serverInfos.add(serverInfo);
                    receLatch.countDown();
                }
            } catch (Exception e) {
            } finally {
                close();
            }
        }

        List<ServerInfo> getServerAndClose() {
            done = true;
            close();
            return serverInfos;
        }

        void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }
    }
}
