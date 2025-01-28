package com.laivyauth.bukkit.impl;

import com.laivyauth.api.LaivyAuthApi;
import com.laivyauth.api.account.Account;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class AuthenticationCommands implements CommandExecutor {

    // Static initializers

    private final @NotNull LaivyAuthApi api;

    public AuthenticationCommands(@NotNull LaivyAuthApi api) {
        this.api = api;
    }

    // Getters

    public @NotNull LaivyAuthApi getApi() {
        return api;
    }

    // Modules

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (sender instanceof Player) {
            @NotNull Player player = (Player) sender;
            @NotNull Account account = getApi().getOrCreate(player.getUniqueId(), player.getName());

            if (command.getName().equalsIgnoreCase("login")) {
                if (args.length != 1) {
                    player.sendMessage("§cWrong number of arguments!");
                } else if (account.isAuthenticated()) {
                    player.sendMessage("§cAlready authenticated!");
                } else if (!account.isRegistered()) {
                    player.sendMessage("§cNot Registered!");
                } else if (!Arrays.equals(account.getPassword(), args[0].toCharArray())) {
                    player.sendMessage("§cWrong password!");
                } else {
                    account.setAuthenticated(true);
                    player.sendMessage("§aWelcome!");
                }
            } else if (command.getName().equalsIgnoreCase("register")) {
                if (args.length != 1) {
                    player.sendMessage("§cWrong number of arguments!");
                } else if (account.isRegistered() || account.isAuthenticated()) {
                    player.sendMessage("§cAlready registered!");
                } else {
                    account.setPassword(args[0].toCharArray());
                    account.setAuthenticated(true);

                    player.sendMessage("§aWelcome!");
                }
            }
        } else {
            sender.sendMessage("§cThis command can only be performed by players!");
        }

        return true;
    }

}
