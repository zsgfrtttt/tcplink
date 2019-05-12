package tcp_udp.util;

import java.io.Closeable;
import java.io.IOException;

public class CloseUtil {
    public static void close(Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable closeable : closeables) {
            try {
                if (closeable != null)
                    closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
