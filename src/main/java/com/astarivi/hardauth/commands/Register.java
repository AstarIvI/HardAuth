package com.astarivi.hardauth.commands;

import com.astarivi.hardauth.HardAuth;
import com.astarivi.hardauth.database.DatabaseQueue;
import com.astarivi.hardauth.utils.PasswordChecker;
import com.astarivi.hardauth.player.PlayerSession;
import com.astarivi.hardauth.player.PlayerStorage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class Register {
    private static boolean shouldPasswordConfirmation = false;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        final boolean confirmPassword = HardAuth.getConfig().getBooleanProperty("registerConfirmPassword", false);

        if (confirmPassword) {
            dispatcher.register(literal("register")
                    .then(argument("password", StringArgumentType.word())
                        .then(argument("passwordConfirmation", StringArgumentType.word())
                            .executes(Register::run))));
            shouldPasswordConfirmation = true;
            return;
        }
        dispatcher.register(literal("register")
                .then(argument("password", StringArgumentType.word())
                    .executes(Register::run)));
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        final String password = StringArgumentType.getString(context, "password");
        ServerPlayerEntity player = source.getPlayer();
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

        if (playerSession.isAuthorized()) {
            source.sendFeedback(Text.of("§cYou are already authorized."), false);
            return 1;
        }

        if (playerSession.getPasswordHash() != null) {
            source.sendFeedback(Text.of("§cYou're already registered."), false);
            return 1;
        }

        if (!isPasswordValid(source, context)) {
            return 1;
        }

        final boolean autologin = HardAuth.getConfig().getBooleanProperty("autoLoginDefault", false);

        playerSession.addUser(password, () -> {
            playerSession.setAuthorized(true);
            playerSession.setSurvival();
            playerSession.setAutoLogin(autologin, false, () -> {
                playerSession.getPlayer().sendMessage(
                        Text.of("§aWelcome.")
                );

                if (playerSession.wasDead()) {
                    playerSession.getPlayer().networkHandler.disconnect(
                            Text.of("You've been disconnected to fix your position, please join again.")
                    );
                }
            });
        });

        return 1;
    }

    private static boolean isPasswordValid(ServerCommandSource source, CommandContext<ServerCommandSource> context) {
        final String password = StringArgumentType.getString(context, "password");

        if (shouldPasswordConfirmation) {
            final String passwordConfirmation = StringArgumentType.getString(context, "passwordConfirmation");
            if (passwordConfirmation.length() == 0) {
                source.sendFeedback(
                    Text.of(
                    "§cPlease confirm your password by typing it again like so:\n\n §3/register <password> <password>"
                    ),
                    false
                );
                return false;
            }

            if (!password.equals(passwordConfirmation)) {
                source.sendFeedback(Text.of("§cBoth passwords must match."), false);
                return false;
            }
        }

        int minCharacters = HardAuth.getConfig().getIntProperty("passwordMinimumLength", 3);
        int maxCharacters = HardAuth.getConfig().getIntProperty("passwordMaximumLength", 50);

        if (password.length() < minCharacters) {
            source.sendFeedback(
                    Text.of(
                            "§cYour password is too short. It must be at least " + minCharacters + " characters."
                    ),
                    false
            );
            return false;
        }

        if (password.length() > maxCharacters) {
            source.sendFeedback(
                    Text.of(
                            "§cYour password is too long. It must be" + maxCharacters + " characters at most."
                    ),
                    false
            );
            return false;
        }

        return true;
    }
}
