package com.csz.link.impl;

import com.csz.link.core.IoProvider;
import com.csz.link.util.CloseUtil;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IoSelectorProvider implements IoProvider {
    private final AtomicBoolean isClose = new AtomicBoolean(false);

    //是否处于某个注册过程
    private final AtomicBoolean inReqInput = new AtomicBoolean(false);
    private final AtomicBoolean inReqOutput = new AtomicBoolean(false);

    private final ExecutorService inputHandlePool;
    private final ExecutorService outputHandlePool;
    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();
    private final Selector readSelector;
    private final Selector writeSelector;

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();
        inputHandlePool = Executors.newFixedThreadPool(4
                , new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(4
                , new IoProviderThreadFactory("IoProvider-Output-Thread-"));

        startRead();
        startWrite();
    }

    private void startRead() {
        Thread thread = new Thread("IoProvider ReadSelector Thread") {
            @Override
            public void run() {
                while (!isClose.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(inReqInput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            selectionKeys.remove(selectionKey);
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool);
                            }
                        }
                    } catch (IOException e) {

                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void startWrite() {
        Thread thread = new Thread("IoProvider WriteSelector Thread") {
            @Override
            public void run() {
                while (!isClose.get()) {
                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(inReqOutput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            selectionKeys.remove(selectionKey);
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool);
                            }
                        }
                    } catch (IOException e) {

                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    @Override
    public boolean registeInput(SocketChannel channel, HandleInputCallbac inputCallbac) {
        return registeSelection(channel, readSelector, SelectionKey.OP_READ, inReqInput, inputCallbackMap, inputCallbac) != null;
    }

    @Override
    public boolean registeOutput(SocketChannel channel, HandleOutputCallback outputCallback) {
        return registeSelection(channel, writeSelector, SelectionKey.OP_WRITE, inReqOutput, outputCallbackMap, outputCallback) != null;
    }

    @Override
    public void unRegisteInput(SocketChannel channel) {
        unRegisteSelection(channel, readSelector, inputCallbackMap);
    }

    @Override
    public void unRegisteOutput(SocketChannel channel) {
        unRegisteSelection(channel, writeSelector, outputCallbackMap);
    }

    @Override
    public void close() throws IOException {
        if (isClose.compareAndSet(false, true)) {
            inputHandlePool.shutdown();
            outputHandlePool.shutdown();
            inputCallbackMap.clear();
            outputCallbackMap.clear();
            readSelector.wakeup();
            writeSelector.wakeup();
            CloseUtil.close(readSelector, writeSelector);
        }
    }

    private void waitSelection(AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static SelectionKey registeSelection(SocketChannel channel, Selector selector, int registerOps,
                                                 AtomicBoolean locker, HashMap<SelectionKey, Runnable> map,
                                                 Runnable runnable) {
        synchronized (locker) {
            locker.set(true);
            try {
                //取消阻塞，更新selector监听，让select()获取最新的监听
                selector.wakeup();

                SelectionKey key = null;
                if (channel.isRegistered()) {
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.interestOps() | registerOps);
                        map.put(key, runnable);
                    }
                }
                if (key == null) {
                    key = channel.register(selector, registerOps);
                    map.put(key, runnable);
                }
                return key;
            } catch (Exception e) {
                return null;
            } finally {
                locker.set(false);
                try {
                    locker.notify();
                } catch (Exception e) {
                }
            }
        }
    }

    private static void unRegisteSelection(SocketChannel channel, Selector selector, HashMap<SelectionKey, Runnable> map) {
        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                key.cancel();
                map.clear();
                selector.wakeup();
            }
        }
    }

    private static void handleSelection(SelectionKey key, int keyOps,
                                        HashMap<SelectionKey, Runnable> map,
                                        ExecutorService pool) {
        //取消对keyOps的监听
        key.interestOps(key.interestOps() & ~keyOps);
        Runnable runnable = null;
        runnable = map.get(key);
        if (runnable != null && !pool.isShutdown()) {
            pool.execute(runnable);
        }
    }

    static class IoProviderThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
