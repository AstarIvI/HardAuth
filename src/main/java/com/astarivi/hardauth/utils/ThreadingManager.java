package com.astarivi.hardauth.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class ThreadingManager {
    private final ScheduledExecutorService schedulerExecutor = Executors.newScheduledThreadPool(1);
    private final ExecutorService databaseExecutor = Executors.newFixedThreadPool(1);

    public ScheduledExecutorService getSchedulerExecutor() {
        return schedulerExecutor;
    }

    public ExecutorService getDatabaseExecutor() {
        return databaseExecutor;
    }

    public void shutdown(){
        schedulerExecutor.shutdownNow();
    }
}
