package udp;

public class MessageCreator {
    public static final String SN_HEADER = "这是一个回送暗号：";
    public static final String RORT_HEADER = "这是一个暗号，请回送信息到端口port：";

    public static String buildWithSn (String sn){
        return SN_HEADER + sn;
    }
    public static String parseSn(String data){
        if (data.startsWith(SN_HEADER)){
            return data.substring(SN_HEADER.length());
        }
        return null;
    }

    public static String buildWithPort (int port){
        return RORT_HEADER + port;
    }
    public static int parsePort(String data){
        if (data.startsWith(RORT_HEADER)){
            return Integer.parseInt(data.substring(RORT_HEADER.length()));
        }
        return -1;
    }
}
