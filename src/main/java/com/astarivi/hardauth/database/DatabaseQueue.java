package com.astarivi.hardauth.database;

import com.astarivi.hardauth.HardAuth;
import com.astarivi.hardauth.utils.ConfigFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;

public class DatabaseQueue {
    private static final Database database;
    static
    {
        final ConfigFile config = HardAuth.getConfig();
        File directory = new File("config/HardAuth/");
        Path path = Paths.get(directory.getAbsolutePath(), config.getProperty("databaseName"));

        database = new Database(
            path.toString(),
            config.getProperty("databaseUser"),
            config.getProperty("databasePassword")
        );

        database.check();
    }

    public static Future<?> submitToQueue(DatabaseTask task){
        return HardAuth.getThreadingManager().getDatabaseExecutor().submit(new DatabaseQueueRunnable(task));
    }

    public static class DatabaseQueueRunnable implements Runnable {
        private final DatabaseTask task;

        public DatabaseQueueRunnable(DatabaseTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            this.task.run(database);
        }
    }

    public interface DatabaseTask {
        void run(Database database);
    }

    public interface DatabaseResult {
        void run();
    }
}
