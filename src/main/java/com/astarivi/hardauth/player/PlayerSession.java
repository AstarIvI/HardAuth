package com.astarivi.hardauth.player;

import com.astarivi.hardauth.database.Database;
import com.astarivi.hardauth.database.DatabaseQueue;
import com.astarivi.hardauth.utils.PasswordChecker;
import com.astarivi.hardauth.utils.RandomRGBColor;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.UUID;

/**
 * Contains player data relevant to authentication, and serves as cache
 * between the database, and the server, thus reducing overhead.
 */

public class PlayerSession {
    private boolean authorized = false;
    private boolean isBlocked = false;
    private boolean hasChangedAutoLogin = false;
    private boolean wasDead = false;
    private boolean autologin;
    private String password;
    private String storedIp;
    private AreaEffectCloudEntity playerCloud;
    private final ServerPlayerEntity player;
    private final UUID uuid;

    public PlayerSession(ServerPlayerEntity player) {
        this.player = player;
        this.uuid = player.getUuid();
        fetchFromDatabase();
    }

    public boolean isBlocked() {
        return this.isBlocked;
    }

    public boolean wasDead() {
        return wasDead;
    }

    public void setWasDead(boolean wasDead) {
        this.wasDead = wasDead;
    }

    public UUID getUUID() {
        return uuid;
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public boolean isAuthorized() {
        if (uuid == null)
            return false;
        return authorized;
    }

    public void setSpectator() {
        player.changeGameMode(GameMode.SPECTATOR);
        playerCloud = new AreaEffectCloudEntity(player.getWorld(), player.getX(), player.getY(), player.getZ());

        //Time is in ticks. It's a ratio of 20 TPS to 1 second. If the server is under heavy load, a.k.a TPS reduction,
        //this timer would be longer than intended. That's not a problem as we want a long timeout anyway.
        playerCloud.setDuration(36000);
        playerCloud.setDurationOnUse(36000);

        //Random Color
        playerCloud.setColor(RandomRGBColor.getColor());

        player.getWorld().spawnEntity(playerCloud);
        player.setCameraEntity(playerCloud);
    }

    public void setSurvival() {
        if (playerCloud != null) {
            playerCloud.setDuration(0);
            playerCloud.setDurationOnUse(0);
            playerCloud = null;
        }

        player.changeGameMode(GameMode.SURVIVAL);
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public String getStoredIp() {
        return storedIp;
    }

    public void updateStoredIp() {
        this.isBlocked = true;

        DatabaseQueue.submitToQueue(
                (database) -> {
                    final String playerIp = this.player.getIp();
                    database.changeIp(this.uuid.toString(), playerIp);
                    this.storedIp = playerIp;

                    this.isBlocked = false;
                }
        );
    }

    public boolean isAutoLogin() {
        return autologin;
    }

    public boolean hasChangedAutoLogin() {
        return hasChangedAutoLogin;
    }

    public void setAutoLogin(boolean value, boolean applyRateLimit, DatabaseQueue.DatabaseResult result) {
        if (this.autologin == value) {
            if (result == null) return;
            result.run();
            return;
        }

        this.isBlocked = true;

        DatabaseQueue.submitToQueue(
                (database) -> {
                    database.changeAutoLogin(uuid.toString(), value);

                    this.autologin = value;
                    if (!applyRateLimit) {
                        this.isBlocked = false;
                        return;
                    }

                    this.hasChangedAutoLogin = true;
                    this.isBlocked = false;
                    result.run();
                }
        );
    }

    public String getPasswordHash() {
        return password;
    }

    public void addUser(String password, DatabaseQueue.DatabaseResult result) {
        this.isBlocked = true;

        DatabaseQueue.submitToQueue(
                (database) -> {
                    String hash;
                    try {
                        hash = PasswordChecker.getSaltedHash(password);
                    } catch (Exception e) {
                        this.isBlocked = false;
                        this.getPlayer().networkHandler.disconnect(
                                Text.of("Your password couldn't be stored. Please reconnect and try with another password.")
                        );
                        return;
                    }

                    database.addUser(
                            this.uuid.toString(),
                            hash,
                            this.player.getIp()
                    );

                    this.password = hash;
                    this.autologin = false;
                    this.storedIp = null;
                    this.isBlocked = false;
                    result.run();
                }
        );
    }

    public void removeUser(DatabaseQueue.DatabaseResult result) {
        this.isBlocked = true;

        DatabaseQueue.submitToQueue(
                (database) -> {
                    database.removeUser(uuid.toString());

                    this.password = null;
                    this.autologin = false;
                    this.storedIp = null;
                    this.isBlocked = false;
                    result.run();
                }
        );
    }

    public void authReminder() {
        PlayerRegisterReminder.addPlayerPendingLogin(player);
    }

    private void fetchFromDatabase() {
        this.isBlocked = true;

        DatabaseQueue.submitToQueue(
            (database) -> {
                final String[] fetchResult = database.fetchUser(this.uuid.toString());
                if (fetchResult == null) {
                    this.isBlocked = false;
                    return;
                }

                this.password = fetchResult[0];
                this.autologin = Boolean.parseBoolean(fetchResult[1]);
                this.storedIp = fetchResult[2];
                this.isBlocked = false;
            }
        );
    }
}