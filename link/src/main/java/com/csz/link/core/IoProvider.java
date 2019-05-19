package com.csz.link.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {
    boolean registeInput(SocketChannel channel, HandleInputCallbac inputCallbac);

    boolean registeOutput(SocketChannel channel, HandleOutputCallback outputCallback);

    void unRegisteInput(SocketChannel channel);

    void unRegisteOutput(SocketChannel channel);

    abstract class HandleInputCallbac implements Runnable {
        @Override
        public void run() {
            canProvideInput();
        }

        public abstract void canProvideInput();
    }

    abstract class HandleOutputCallback implements Runnable {

        private Object attach;

        @Override
        public void run() {
            canProvideOutput(attach);
        }

        public final void setAttach(Object object) {
            attach = object;
        }

        public abstract void canProvideOutput(Object attach);
    }
}
