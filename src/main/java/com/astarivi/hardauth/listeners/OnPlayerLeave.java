package com.astarivi.hardauth.listeners;

import com.astarivi.hardauth.player.PlayerSession;
import com.astarivi.hardauth.player.PlayerStorage;

import net.minecraft.server.network.ServerPlayerEntity;

public class OnPlayerLeave {
    
    public static void listen(ServerPlayerEntity player) {
        PlayerSession playerSession = PlayerStorage.getPlayerSession(player.getUuid());
        if (playerSession != null){
            playerSession.setSurvival();
        }
        PlayerStorage.removePlayerSession(player.getUuid());
    }
}
