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
        System.out.println(">HardAuth: Shutting down scheduler threads.");
        schedulerExecutor.shutdownNow();
        System.out.println(">>HardAuth: Syncing database before quitting. Quitting now may result in database corruption.");
        databaseExecutor.shutdown();
    }
}
