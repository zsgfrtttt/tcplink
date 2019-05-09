package tcp_udp.util;

public class ByteUtil {

    public static boolean startWith(byte[] receData, byte[] header) {
        if (receData != null && header != null && receData.length >= header.length){
            for (int i=0;i< header.length;i++){
                if (header[i] != receData[i]){
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
