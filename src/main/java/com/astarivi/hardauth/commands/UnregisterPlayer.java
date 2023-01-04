package com.astarivi.hardauth.commands;

import com.astarivi.hardauth.player.PlayerSession;
import com.astarivi.hardauth.player.PlayerStorage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;


public class UnregisterPlayer{

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("unregisterplayer")
                .requires(source -> source.hasPermissionLevel(2))
                    .then(argument("player", word())
                        .executes(UnregisterPlayer::run)));
    }

    private static int run(CommandContext<ServerCommandSource> context){
        final PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
        final ServerCommandSource source = context.getSource();
        ServerPlayerEntity targetPlayer = playerManager.getPlayer(getString(context, "player"));

        if (targetPlayer == null){
            source.sendFeedback(
                Text.of("§cThe specified player does not exist or is not online."),
                false
            );
            return 1;
        }

        PlayerSession playerSession = PlayerStorage.getPlayerSession(targetPlayer.getUuid());

        if (playerSession == null){
            //This could be solved by creating a PlayerSession without using the PlayerStorage.
            source.sendFeedback(
                Text.of("§cThe specified player has not been found in the cache. Try to have them rejoin."),
                false
            );
            return 1;
        }

        playerSession.removeUser();
        playerSession.setAuthorized(false);
        playerSession.getPlayer().networkHandler.disconnect(
                Text.of("An administrator has reset your credentials. Please rejoin and register again.")
        );

        source.sendFeedback(
                Text.of("§aThe user credentials have been removed, and the user has been de-authorized."),
                false
        );


        return 1;
    }
}