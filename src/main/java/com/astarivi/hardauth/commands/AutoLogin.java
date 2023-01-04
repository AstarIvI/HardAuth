package com.astarivi.hardauth.commands;

import com.astarivi.hardauth.player.PlayerSession;
import com.astarivi.hardauth.player.PlayerStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class AutoLogin{

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("autologin")
                .executes(AutoLogin::run));
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = context.getSource().getPlayer();
        PlayerSession playerSession = PlayerStorage.getPlayerSession(player.getUuid());

        if (playerSession == null) return 1;

        if (playerSession.hasChangedAutoLogin()) {
            source.sendFeedback(Text.of(
                            "§cYou can only change this setting once per game session.\n§eTo change your autologin value, please rejoin the server."),
                    false
            );
            return 1;
        }

        playerSession.setAutoLogin(!playerSession.isAutoLogin(), true);
        final String feedback = playerSession.isAutoLogin() ? "activated." : "deactivated.";
        source.sendFeedback(Text.of("§aAutologin has been " + feedback), false);

        return 1;
    }
}
