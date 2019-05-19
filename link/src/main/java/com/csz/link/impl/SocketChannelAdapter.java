package com.csz.link.impl;

import com.csz.link.core.IReceiver;
import com.csz.link.core.ISender;
import com.csz.link.core.IoArgs;
import com.csz.link.core.IoProvider;
import com.csz.link.util.CloseUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements IReceiver, ISender, Closeable {
    private final AtomicBoolean isClose = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final ChannalCloseCallback channalCloseCallback;

    private IoArgs.IoArgsEventListener receiveIoListener;
    private IoArgs.IoArgsEventListener sendIoListener;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, ChannalCloseCallback channalCloseCallback) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.channalCloseCallback = channalCloseCallback;

        this.channel.configureBlocking(false);
    }

    @Override
    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClose.get()) {
            throw new IOException("current channal is closed.");
        }
        receiveIoListener = listener;
        return ioProvider.registeInput(channel, handleInputCallback);
    }

    @Override
    public boolean sendAsync(IoArgs ioArgs, IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClose.get()) {
            throw new IOException("current channal is closed.");
        }
        sendIoListener = listener;
        handleOutputCallback.setAttach(ioArgs);
        return ioProvider.registeOutput(channel, handleOutputCallback);
    }

    @Override
    public void close() {
        if (isClose.compareAndSet(false, true)) {
            ioProvider.unRegisteInput(channel);
            ioProvider.unRegisteOutput(channel);
            CloseUtil.close(channel);
            channalCloseCallback.onChannelClose(channel);
        }
    }

    public interface ChannalCloseCallback {
        void onChannelClose(SocketChannel channel);
    }

    public IoProvider.HandleInputCallbac handleInputCallback = new IoProvider.HandleInputCallbac() {
        @Override
        public void canProvideInput() {
            if (isClose.get()) {
                return;
            }
            IoArgs args = new IoArgs();
            if (receiveIoListener != null) {
                receiveIoListener.onStart(args);
            }

            try {
                if (args.read(channel) > 0 && receiveIoListener != null) {
                    receiveIoListener.onCompleted(args);
                } else {
                    throw new IOException("can not read data!");
                }
            } catch (IOException e) {
                CloseUtil.close(SocketChannelAdapter.this::close);
            }
        }
    };

    public IoProvider.HandleOutputCallback handleOutputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        public void canProvideOutput(Object attach) {
            if (isClose.get()) {
                return;
            }
            sendIoListener.onCompleted(null);
        }
    };
}
