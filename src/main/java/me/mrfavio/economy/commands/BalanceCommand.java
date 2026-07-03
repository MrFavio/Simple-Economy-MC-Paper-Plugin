package me.mrfavio.economy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.mrfavio.economy.Economy;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class BalanceCommand implements CommandExecutor {

    private final Economy plugin;

    public  BalanceCommand(Economy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,  String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        double balance = plugin.getBalance(player.getUniqueId());
        player.sendMessage("§aYour balance: §e" + balance + "$");
        return true;
    }
}
