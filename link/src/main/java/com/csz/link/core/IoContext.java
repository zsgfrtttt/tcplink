package com.csz.link.core;

import java.io.IOException;

public class IoContext{
    private static IoContext INSTANCE;
    private final IoProvider ioProvider;

    private IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    public static IoContext getInstance(){
        return INSTANCE;
    }

    public static StartBoot setup(){
        return new StartBoot();
    }

    public static void close() throws IOException {
        if (INSTANCE != null){
            INSTANCE.callClose();
        }
    }

    private void callClose() throws IOException {
        ioProvider.close();
    }

    public static class StartBoot{
        private IoProvider ioProvider;
        private StartBoot(){}

        public StartBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public IoContext start(){
            INSTANCE = new IoContext(ioProvider);
            return INSTANCE;
        }
    }
}
