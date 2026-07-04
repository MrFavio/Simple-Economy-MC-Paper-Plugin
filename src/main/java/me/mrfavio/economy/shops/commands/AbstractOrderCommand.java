package me.mrfavio.economy.shops.commands;

import me.mrfavio.economy.shops.ShopManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public abstract class AbstractOrderCommand implements CommandExecutor {

    protected final ShopManager shopManager;
    private final String type;
    private final boolean isAdmin;
    private final String commandName;

    protected AbstractOrderCommand(ShopManager shopManager, String type, boolean isAdmin, String commandName) {
        this.shopManager = shopManager;
        this.type = type;
        this.isAdmin = isAdmin;
        this.commandName = commandName;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        }

        if (isAdmin && !player.hasPermission("economy.admin")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        String action = type.equals("BUY") ? "buy" : "sell";

        if (args.length < 2) {
            player.sendMessage("§cUsage: /" + commandName + " <amount> <price>  (hold the item you want the shop to " + action + " in your hand)");
            return true;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            player.sendMessage("§cYou must be holding the item you want the shop to " + action + "!");
            return true;
        }

        int amount, price;
        try {
            amount = Integer.parseInt(args[0]);
            price = Integer.parseInt(args[1]);
            if (amount <= 0 || price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§cAmount and price must be positive numbers!");
            return true;
        }

        Block chestBlock = player.getTargetBlockExact(5);
        if (chestBlock == null || chestBlock.getType() != Material.CHEST) {
            player.sendMessage("§cYou must be looking at a chest (max 5 blocks away)!");
            return true;
        }

        ItemStack template = handItem.clone();
        template.setAmount(1);

        Sign sign = shopManager.createShopSign(player, chestBlock, type, template, amount, price, isAdmin);
        if (sign == null) return true;

        shopManager.createShop(chestBlock.getLocation(), player.getUniqueId(), type, template, amount, price, sign.getLocation(), isAdmin);
        player.sendMessage(successMessage());
        return true;
    }

    protected abstract String successMessage();
}