package me.mrfavio.economy.listeners;

import me.mrfavio.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final Economy plugin;

    public PlayerJoinListener(Economy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.hasAccount(uuid)) {
            plugin.setBalance(uuid, 100.0);
        }

        plugin.updateTabBalance(player);
    }
}
