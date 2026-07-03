package me.mrfavio.economy.listeners;

import me.mrfavio.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;


public class PlayerQuitListener implements Listener {

    private final Economy plugin;

    public PlayerQuitListener(Economy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.saveBalancesData();
    }
}
