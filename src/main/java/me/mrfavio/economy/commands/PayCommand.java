package me.mrfavio.economy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.mrfavio.economy.Economy;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class PayCommand implements CommandExecutor {

    private final Economy plugin;

    public PayCommand(Economy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can send money.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /pay <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cThat player is offline.");
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage("§cAmount must be greater than zero!");
                return true;
            }

            double senderBalance = plugin.getBalance(player.getUniqueId());
            if (senderBalance < amount) {
                player.sendMessage("§cYou do not have enough money!");
                return true;
            }

            plugin.setBalance(player.getUniqueId(), senderBalance - amount);
            plugin.setBalance(target.getUniqueId(), plugin.getBalance(target.getUniqueId()) + amount);

            player.sendMessage("§aYou sent §e" + amount + "$ §ato §f" + target.getName());
            target.sendMessage("§aYou received §e" + amount + "$ §afrom §f" + player.getName());
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount! Please enter a valid number.");
        }
        return true;
    }
}
