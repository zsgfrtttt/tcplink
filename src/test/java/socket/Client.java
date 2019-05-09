package socket;

import java.io.*;
import java.net.*;

public class Client {

    public static void main(String[] args) throws Exception{
        Socket socket = new Socket();
        //设置读取服务器端信息超时
        socket.setSoTimeout(5000);
        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(),2000),3000);
        System.out.println("已经链接服务器。");
        System.out.println("客户端信息："+socket.getLocalAddress() +" "+socket.getLocalPort());
        System.out.println("服务端信息："+socket.getInetAddress() + " "+socket.getPort());

        todo(socket);

        socket.close();
        System.out.println("客户端exit。");
    }

    private static void todo(Socket client) throws Exception{
        InputStream is = System.in;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

        OutputStream outputStream = client.getOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        InputStream inputStream = client.getInputStream();
        BufferedReader ServerReader = new BufferedReader(new InputStreamReader(inputStream));

        boolean conti = true;
        do {
            String line = bufferedReader.readLine();
            printStream.println(line);

            String serverLine = ServerReader.readLine();
            if ("bye".equalsIgnoreCase(serverLine)) {
                conti = false;
            }else{
                System.out.println(serverLine);
            }
        }while (conti);

        ServerReader.close();
        printStream.close();

    }

    private void initSocket() throws Exception{
        //设置服务器代理
        Socket socket = new Socket(new Proxy(Proxy.Type.HTTP,new InetSocketAddress(InetAddress.getByName("www.baidu.com"),8080)));

        //一般端口被占用后close之后的两分钟不能直接使用，设置这个属性可以直接使用
        socket.setReuseAddress(true);

        //TcpNoDelay=false，为启用nagle算法,把小数据包写入缓存，避免多次回传占用带宽
        socket.setTcpNoDelay(false);

        //在2小时无响应的情况下发送心跳获取应答，无应答抛异常断链接
        socket.setKeepAlive(true);

        //false,0    close后立马返回，系统接管输出流，把缓冲数据发送完成、
        //true,o     close后立即返回并且丢弃缓冲数据，发送RST命令，并无需2MSL等待
        //true，200  close后在200毫秒内把缓冲数据发送（阻塞），超时后按（true，0）对待
        socket.setSoLinger(false,0);

        //发送紧急数据，低8位 ，不影响系统的readbuffer
        socket.sendUrgentData(1);
        //让紧急数据内敛（buffer）能读到   默认false
        socket.setOOBInline(true);

        //设置接受，发送的拆分宝数据大小
        socket.setReceiveBufferSize(64*1024);
        socket.setSendBufferSize(64*1024);

        //性能权重：链接时间，延迟，带宽
        socket.setPerformancePreferences(1,1,1);
    }
}
