package codes.laivy.auth.impl;

import codes.laivy.auth.LaivyAuth;
import codes.laivy.auth.core.Account;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class SpigotListener implements Listener {

    // Object

    @EventHandler
    private void quit(@NotNull PlayerQuitEvent e) {
        @NotNull Account data = LaivyAuth.getApi().getAccount(e.getPlayer().getUniqueId()).orElseThrow(NullPointerException::new);
        data.setAuthenticated(true);

        Bukkit.getConsoleSender().sendMessage("§8(§c-§8) Player §7" + e.getPlayer().getName() + "§8 left the server");
        e.setQuitMessage(null);
        // todo: reset title
    }
    @EventHandler
    private void join(@NotNull PlayerJoinEvent e) {
        @NotNull Account data = LaivyAuth.getApi().getAccount(e.getPlayer().getUniqueId()).orElseThrow(NullPointerException::new);

        if (e.getPlayer().isOnline()) {
            e.getPlayer().setWalkSpeed(0.2f);
            e.getPlayer().setFlySpeed(0.1f);

            e.setJoinMessage(null);
            // todo: reset title

            if (data.isNew()) {
                // todo: message.yml
                Bukkit.getConsoleSender().sendMessage("§8(§a+§8) §a§l§oNEW§8 Player §7" + e.getPlayer().getName() + "§8 logged in with address §7" + e.getPlayer().getAddress() + "§8.");
            } else {
                // todo: message.yml
                Bukkit.getConsoleSender().sendMessage("§8(§a+§8) Player §7" + e.getPlayer().getName() + "§8 logged in with address §7" + e.getPlayer().getAddress() + "§8.");
            }
        }
    }

}   