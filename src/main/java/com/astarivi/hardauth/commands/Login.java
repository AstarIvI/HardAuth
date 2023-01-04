package com.astarivi.hardauth.commands;

import com.astarivi.hardauth.utils.PasswordChecker;
import com.astarivi.hardauth.player.PlayerSession;
import com.astarivi.hardauth.player.PlayerStorage;

import com.mojang.brigadier.CommandDispatcher;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;

import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class Login {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("login")
                .then(argument("password", word())
                    .executes(Login::run)));
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        final String password = getString(context, "password");
        ServerPlayerEntity player = source.getPlayer();
        PlayerSession playerSession = PlayerStorage.getPlayerSession(player.getUuid());

        if (playerSession == null) return 1;

        if (playerSession.isAuthorized()) {
            source.sendFeedback(Text.of("§cYou are already authorized."), false);
            return 1;
        }

        String hash = playerSession.getPasswordHash();

        if (hash == null) {
            source.sendFeedback(Text.of("§cYou're not registered! Use /register instead."), false);
            return 1;
        } else if (password.length() == 0){
            source.sendFeedback(Text.of("§cYour password cannot be empty."), false);
            return 1;
        }

        Vec3d playerPosition = player.getPos();

        try {
            if (PasswordChecker.check(password, hash)) {
                playerSession.setAuthorized(true);
                playerSession.setSurvival();
                source.sendFeedback(Text.of("§aLogged in"), false);
                player.networkHandler.sendPacket(
                        new PlaySoundS2CPacket(
                                RegistryEntry.of(
                                        SoundEvent.of(
                                            new Identifier("minecraft:block.note_block.pling")
                                )),
                                SoundCategory.MASTER,
                                playerPosition.x,
                                playerPosition.y,
                                playerPosition.z,
                                100f,
                                0f,
                                0
                        )
                );

                // The player had to log in as their IP has changed.
                if (playerSession.isAutoLogin()) {
                    playerSession.updateStoredIp();
                }

            } else {
                player.networkHandler.sendPacket(
                        new PlaySoundS2CPacket(
                                RegistryEntry.of(
                                        SoundEvent.of(
                                                new Identifier("minecraft:entity.zombie.attack_iron_door")
                                        )),
                                SoundCategory.MASTER,
                                playerPosition.x,
                                playerPosition.y,
                                playerPosition.z,
                                20f,
                                0.5f,
                                0
                        )
                );
                source.sendFeedback(Text.of("§cIncorrect password, please try again."), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            playerSession.removeUser();
            playerSession.setAuthorized(false);
            playerSession.getPlayer().networkHandler.disconnect(Text.of("Your password has been reset, please rejoin and register again."));
        }

        if (playerSession.wasDead()) {
            playerSession.getPlayer().networkHandler.disconnect(Text.of("As you've been respawned, please join again."));
        }

        return 1;
    }
}