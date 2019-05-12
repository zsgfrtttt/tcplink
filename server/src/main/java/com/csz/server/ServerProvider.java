package com.csz.server;

import com.csz.foo.contants.UDPContants;
import com.csz.link.util.ByteUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ServerProvider {
    private static Provider PROVIDER_INSTANCE;

    public static void start(int portServer) {
        stop();
        String sn = UUID.randomUUID().toString();
        System.out.println("uuid:"+sn);
        Provider provider = new Provider(sn, portServer);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    public static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static final class Provider extends Thread {

        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket datagramSocket;
        private final byte[] buffer = new byte[128];

        public Provider(String sn, int portServer) {
            this.sn = sn.getBytes();
            this.port = portServer;
        }

        @Override
        public void run() {
            try {
                System.out.println("provider start..");
                //监听端口
                datagramSocket = new DatagramSocket(UDPContants.PORT_SERVER);
                while (!done) {
                    DatagramPacket rece = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(rece);
                    int port = rece.getPort();
                    String hostName = rece.getAddress().getHostName();
                    byte[] receData = rece.getData();
                    int length = rece.getLength();
                    //7报头 2命令 4端口
                    boolean isVaild = length >= UDPContants.HEADER.length + 2 + 4 && ByteUtil.startWith(receData, UDPContants.HEADER);
                    if (!isVaild) {
                        continue;
                    }

                    int index = UDPContants.HEADER.length;
                    short cmd = (short) ((receData[index++] << 8) | (receData[index++] & 0xFF));
                    int responsePort = (receData[index++] << 24) |
                                        (receData[index++] << 16) |
                                        (receData[index++] << 8) |
                                        receData[index++];

                    if (cmd == 1 && responsePort > 0) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPContants.HEADER);
                        byteBuffer.putShort((short) 2);
                        byteBuffer.putInt(this.port);
                        byteBuffer.put(sn);
                        DatagramPacket send = new DatagramPacket(buffer, byteBuffer.position(), rece.getAddress(), responsePort);
                        datagramSocket.send(send);
                        System.out.println("服务器回送数据： " + " port：" + responsePort);
                    } else {
                        System.out.println("数据不合法！");
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
