package codes.laivy.auth.bukkit.impl;

import codes.laivy.auth.account.Account;
import codes.laivy.auth.bukkit.LaivyAuth;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

// todo: message.yml
public final class SpigotListener implements Listener {

    // Static initializers

    public static boolean isLogged(@NotNull Player player) {
        return LaivyAuth.getApi().getAccount(player.getUniqueId()).orElseThrow(() -> new NullPointerException("cannot find the account for player '" + player.getName() + "'")).isAuthenticated();
    }

    // Object

    @EventHandler(priority = EventPriority.LOW)
    private void quit(@NotNull PlayerQuitEvent e) {
        @NotNull Account data = LaivyAuth.getApi().getAccount(e.getPlayer().getUniqueId()).orElseThrow(() -> new NullPointerException("cannot find the account for player '" + e.getPlayer().getName() + "'"));
        data.setAuthenticated(false);

        Bukkit.getConsoleSender().sendMessage("§8(§c-§8) Player §7" + e.getPlayer().getName() + "§8 left the server");

        e.setQuitMessage(null);
        // todo: reset title
    }
    @EventHandler(priority = EventPriority.LOW)
    private void join(@NotNull PlayerJoinEvent e) {
        @NotNull Account data = LaivyAuth.getApi().getAccount(e.getPlayer().getUniqueId()).orElseThrow(() -> new NullPointerException("cannot find the account for player '" + e.getPlayer().getName() + "'"));

        if (data.getType() == Account.Type.PREMIUM) {
            data.setAuthenticated(true); // Automatic authenticate premium users
        }

        if (e.getPlayer().isOnline()) {
            e.getPlayer().setWalkSpeed(0.2f);
            e.getPlayer().setFlySpeed(0.1f);

            e.setJoinMessage(null);
            // todo: reset title

            if (data.isNew()) {
                Bukkit.getConsoleSender().sendMessage("§8(§a+§8) §a§l§oNEW§8 Player §7" + e.getPlayer().getName() + "§8 logged in with address §7" + e.getPlayer().getAddress() + "§8.");
            } else {
                Bukkit.getConsoleSender().sendMessage("§8(§a+§8) Player §7" + e.getPlayer().getName() + "§8 logged in with address §7" + e.getPlayer().getAddress() + "§8.");
            }
        }
    }

    @EventHandler
    private void target(@NotNull EntityTargetLivingEntityEvent e) {
        if (e.getTarget() instanceof Player) {
            @NotNull Player player = (Player) e.getTarget();

            if (!isLogged(player)) {
                e.setCancelled(true);
            }
        }
    }
    @EventHandler
    private void tabComplete(@NotNull PlayerChatTabCompleteEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.getTabCompletions().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void chat(@NotNull AsyncPlayerChatEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.getPlayer().sendMessage("§cYou must be authenticated to use chat!");
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void preProcess(@NotNull PlayerCommandPreprocessEvent e) {
        @NotNull List<String> availableCommands = Arrays.asList("login", "register");

        String inputMessage = e.getMessage();
        String commandName = inputMessage.split(" ")[0].replace("/", "");

        if (!availableCommands.contains(commandName)) {
            Player player = e.getPlayer();
            if (!isLogged(e.getPlayer())) {
                player.sendMessage("§cYou must be authenticated to perform that command.");
                e.setCancelled(true);
            }
        }

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void damaged(@NotNull EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            if (!isLogged((Player) e.getEntity())) {
                e.setCancelled(true);
            }
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void damageOthers(@NotNull EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            if (!isLogged((Player) e.getDamager())) {
                e.setCancelled(true);
            }
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void bedEnter(@NotNull PlayerBedEnterEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.getPlayer().sendMessage("§cYou must be authenticated to do this!");
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void interact(@NotNull PlayerInteractEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void bucket(@NotNull PlayerBucketEmptyEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.getPlayer().sendMessage("§cYou must be authenticated to do this!");
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void bucket(@NotNull PlayerBucketFillEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.getPlayer().sendMessage("§cYou must be authenticated to do this!");
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void dropItem(@NotNull PlayerDropItemEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.getPlayer().sendMessage("§cYou must be authenticated to do this!");
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void interactEntity(@NotNull PlayerInteractEntityEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.getPlayer().sendMessage("§cYou must be authenticated to do this!");
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void open(@NotNull InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player) {
            @NotNull Player player = (Player) e.getPlayer();

            if (!isLogged(player)) {
                player.sendMessage("§cYou must be authenticated to do this!");
                e.setCancelled(true);
            }
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void click(@NotNull InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            @NotNull Player player = (Player) e.getWhoClicked();

            if (!isLogged(player)) {
                player.sendMessage("§cYou must be authenticated to do this!");
                e.setCancelled(true);
            }
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void pickupItem(@NotNull PlayerPickupItemEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void blockPlace(@NotNull BlockPlaceEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.getPlayer().sendMessage("§cYou must be authenticated to do this!");
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void blockBreak(@NotNull BlockBreakEvent e) {
        if (!isLogged(e.getPlayer())) {
            e.getPlayer().sendMessage("§cYou must be authenticated to do this!");
            e.setCancelled(true);
        }
    }

}   