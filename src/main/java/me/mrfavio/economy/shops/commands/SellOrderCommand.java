package me.mrfavio.economy.shops.commands;

import me.mrfavio.economy.shops.ShopManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class SellOrderCommand implements CommandExecutor {

    private final ShopManager shopManager;

    public SellOrderCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /sellorder <item> <amount> <price>");
            return true;
        }

        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) {
            player.sendMessage("§cInvalid item material name!");
            return true;
        }

        int amount, price;
        try {
            amount = Integer.parseInt(args[1]);
            price = Integer.parseInt(args[2]);
            if (amount <= 0 || price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§cAmount and price must be positive numbers!");
            return true;
        }

        org.bukkit.block.Block chestBlock = player.getTargetBlockExact(5);
        if (chestBlock == null || chestBlock.getType() != Material.CHEST) {
            player.sendMessage("§cYou must be looking at a chest (max 5 blocks away)!");
            return true;
        }

        org.bukkit.block.Sign sign = shopManager.createShopSign(player, chestBlock, "SELL", mat, amount, price, false);
        if (sign == null) return true;

        shopManager.createShop(chestBlock.getLocation(), player.getUniqueId(), "SELL", mat.name(), amount, price, sign.getLocation(), false);
        player.sendMessage("§aSuccessfully created a selling shop!");
        return true;
    }
}