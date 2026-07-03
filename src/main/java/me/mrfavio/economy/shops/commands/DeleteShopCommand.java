package me.mrfavio.economy.shops.commands;

import me.mrfavio.economy.shops.ShopManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class DeleteShopCommand implements CommandExecutor {

    private final ShopManager shopManager;

    public DeleteShopCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            player.sendMessage("§cYou must be looking at a shop chest!");
            return true;
        }

        ShopManager.ShopData shop = shopManager.getShop(targetBlock.getLocation());
        if (shop == null) {
            player.sendMessage("§cThis chest is not registered as a shop!");
            return true;
        }

        if (!shop.owner.equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage("§cYou do not own this shop!");
            return true;
        }

        Block signBlock = shop.signLocation.getBlock();
        if (signBlock.getType().name().contains("SIGN")) {
            signBlock.setType(Material.AIR);
        }

        shopManager.removeShop(targetBlock.getLocation());
        player.sendMessage("§aThe shop has been removed successfully. Chest contents were not modified!");
        return true;
    }
}