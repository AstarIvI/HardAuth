package com.astarivi.hardauth;

import com.astarivi.hardauth.commands.*;
import com.astarivi.hardauth.database.Database;
import com.astarivi.hardauth.player.PlayerRegisterReminder;
import com.astarivi.hardauth.player.PlayerSession;
import com.astarivi.hardauth.player.PlayerStorage;
import com.astarivi.hardauth.utils.ConfigFile;

import com.astarivi.hardauth.utils.ThreadingManager;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

public class HardAuth implements DedicatedServerModInitializer {
    private static final ConfigFile config = new ConfigFile();
    private static final ThreadingManager threadingManager = new ThreadingManager();

    @Override
    public void onInitializeServer() {
        PlayerStorage.initialize();
        initializeDatabase();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            Login.register(dispatcher);
            Register.register(dispatcher);
            Unregister.register(dispatcher);
            UnregisterPlayer.register(dispatcher);
            AutoLogin.register(dispatcher);
        });
        PlayerRegisterReminder.initialize(threadingManager);

        ServerLifecycleEvents.SERVER_STOPPED.register(this::onStop);
    }

    private void onStop(MinecraftServer server) {
        threadingManager.shutdown();
        config.save();
    }

    public static ConfigFile getConfig() {
        return config;
    }

    public static ThreadingManager getThreadingManager() {
        return threadingManager;
    }

    private void initializeDatabase(){
        File directory = new File("config/HardAuth/");
        Path path = Paths.get(directory.getAbsolutePath(), config.getProperty("databaseName"));
        
        Database database = new Database(
                path.toString(),
                config.getProperty("databaseUser"),
                config.getProperty("databasePassword")
        );

        database.check();
        PlayerSession.setDatabase(database);
    }
}
