package com.csz.link.core;

import java.io.Closeable;
import java.io.IOException;

public interface IReceiver extends Closeable {
    boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException;
}
