package com.astarivi.hardauth.listeners;

import com.astarivi.hardauth.player.PlayerSession;
import com.astarivi.hardauth.player.PlayerStorage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;


public class OnPlayerConnect {

    public static void listen(ServerPlayerEntity player) {
        PlayerSession playerSession = new PlayerSession(player);

        if(player.isDead()){
            if (player.getHealth() > 0.0F) {
                player.setHealth(0.0F);
            }
            MinecraftServer server = player.getServer();
            if (server == null) return;
            PlayerManager playerManager = server.getPlayerManager();

            // TODO Will throw exception, apparently.
            playerManager.respawnPlayer(player, true);
            playerSession.setWasDead(true);
        }

        if (playerSession.isAutoLogin() && player.getIp().equals(playerSession.getStoredIp()) && !playerSession.wasDead()){
            playerSession.setSurvival();
            playerSession.setAuthorized(true);
            player.sendMessage(Text.of(
                    "§9You have been logged in automatically. Use /autologin to disable this function.\n§aWelcome back."),
                    false
            );
        } else {
            playerSession.setSpectator();
            playerSession.authReminder();
            player.sendMessage(Text.of(
                    "§9Welcome to this shithole, to play, you must auth in.\n§eLog in using /login and register using /register"),
                    false
            );
        }
        PlayerStorage.addPlayerSession(playerSession);
    }
}
