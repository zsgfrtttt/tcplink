package com.csz.link.core;

import java.io.Closeable;
import java.io.IOException;

public interface ISender extends Closeable {
    boolean sendAsync(IoArgs ioArgs, IoArgs.IoArgsEventListener listener) throws IOException;
}
