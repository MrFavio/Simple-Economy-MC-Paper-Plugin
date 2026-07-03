package me.mrfavio.economy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.mrfavio.economy.Economy;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class EconomyCommand implements CommandExecutor {

    private final Economy plugin;

    public EconomyCommand(Economy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NonNull [] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /eco <give/take/set> <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cThat player is offline.");
            return true;
        }

        try {
            String action = args[0].toLowerCase();
            double amount = Double.parseDouble(args[2]);
            double currentBalance = plugin.getBalance(target.getUniqueId());

            switch (action) {
                case "give" -> {
                    plugin.setBalance(target.getUniqueId(), currentBalance + amount);
                    int displayAmount = (int) amount;
                    sender.sendMessage("§aAdded §e%d$ §ato %s's balance.".formatted(displayAmount, target.getName()));
                }
                case "take" -> {
                    plugin.setBalance(target.getUniqueId(), currentBalance - amount);
                    int displayAmount = (int) amount;
                    sender.sendMessage("§cTook §e%d$ §cfrom %s's balance.".formatted(displayAmount, target.getName()));
                }
                case "set" -> {
                    plugin.setBalance(target.getUniqueId(), amount);
                    int displayAmount = (int) amount;
                    sender.sendMessage("§aSet %s's balance to §e%d$".formatted(target.getName(), displayAmount));
                }
                default -> sender.sendMessage("§cUnknown action! Use: give, take, or set.");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount! Please enter a valid number.");
        }
        return true;
    }
}
