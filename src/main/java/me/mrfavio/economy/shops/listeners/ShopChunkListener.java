package me.mrfavio.economy.shops.listeners;

import me.mrfavio.economy.shops.ShopManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ShopChunkListener implements Listener {

    private final ShopManager shopManager;

    public ShopChunkListener(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        shopManager.resyncChunkDisplays(event.getChunk());
    }
}