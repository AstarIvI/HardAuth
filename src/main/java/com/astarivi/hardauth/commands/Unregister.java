package com.astarivi.hardauth.commands;

import com.astarivi.hardauth.database.DatabaseQueue;
import com.astarivi.hardauth.player.PlayerSession;
import com.astarivi.hardauth.player.PlayerStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class Unregister{

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("unregister")
                .executes(Unregister::run));
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = context.getSource().getPlayer();
        PlayerSession playerSession = PlayerStorage.getPlayerSession(player.getUuid());

        if (playerSession == null) return 1;

        if (playerSession.isBlocked()) {
            source.sendFeedback(Text.of(
                            "§cWe couldn't process your current request right now, as your last request is still " +
                                    "being processed. Please try again later."),
                    false
            );
            return 1;
        }

        playerSession.removeUser(() -> {
            playerSession.setAuthorized(false);
            playerSession.getPlayer().networkHandler.disconnect(Text.of("Successfully unregistered, please rejoin and register again."));
        });

        return 1;
    }
}
