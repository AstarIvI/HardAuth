package com.astarivi.hardauth.player;

import com.astarivi.hardauth.utils.ThreadingManager;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

/**
 * Sends reminders to non-authenticated players, or kicks them after a certain
 * number of repetitions.
 */

public class PlayerRegisterReminder {
    private static final List<PlayerPendingLogin> playersPendingLogin = new ArrayList<>();

    public static void initialize(ThreadingManager manager){
        manager.getSchedulerExecutor().scheduleAtFixedRate(new PlayerRegisterReminderRunnable(), 0, 12, TimeUnit.SECONDS);
    }

    public static void addPlayerPendingLogin(ServerPlayerEntity player){
        playersPendingLogin.add(new PlayerPendingLogin(player));
    }

    public static List<PlayerPendingLogin> getPlayersPendingLogin(){
        return playersPendingLogin;
    }

    private static class PlayerRegisterReminderRunnable implements Runnable {
        public PlayerRegisterReminderRunnable() {
        }

        @Override
        public void run() {
            List<PlayerPendingLogin> playerList = getPlayersPendingLogin();
            ListIterator<PlayerPendingLogin> playerIterator = playerList.listIterator();

            while (playerIterator.hasNext()) {
                PlayerPendingLogin playerPending = playerIterator.next();
                ServerPlayerEntity player = playerPending.player;
                try {
                    PlayerSession playerSession = PlayerStorage.getPlayerSession(player.getUuid());

                    if (playerSession == null || !player.networkHandler.getConnection().isOpen()) {
                        playerIterator.remove();
                        continue;
                    }

                    if (playerSession.isAuthorized()) {
                        playerIterator.remove();
                        continue;
                    }

                    if (playerPending.reminderCounter > 9) {
                        player.networkHandler.disconnect(Text.of(
                                "You took too long to authenticate, please try again."
                        ));
                    }
                    playerPending.reminderCounter++;

                    final String passwordHash = playerSession.getPasswordHash();

                    if (passwordHash == null) {
                        player.sendMessage(
                                Text.of("§3Please register by using §c/register.\n§3E.g. §c/register <password>"),
                                false
                        );
                    } else {
                        player.sendMessage(
                                Text.of("§3To continue, please login by using §c/login."),
                                false
                        );
                    }
                } catch (Exception e) {
                    playerIterator.remove();
                }
            }
        }
    }

    public static class PlayerPendingLogin{
        public ServerPlayerEntity player;
        public int reminderCounter = 0;

        public PlayerPendingLogin(ServerPlayerEntity p){
            player = p;
        }
    }
}