package me.mrfavio.economy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class InfoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        player.sendMessage(
                """
                §a/info - This command gives you all needed information about this plugin's commands
                §a/balance - Allows you to check your current bank balance
                §a/pay <player> <amount> - Allows you to transfer your money to other player
                §a/buyorder <item> <amount> <price> - Allows you to setup a shop where you can buy items (do this while looking at the chest)
                §a/sellorder <item> <amount> <price> - Allows you to setup a shop where you can sell items (do this while looking at the chest)
                §a/deleteshop - Allows you to delete your shop""");
        if (sender.hasPermission("economy.admin")) {
            player.sendMessage(
                    """
                    §eAdmin's commands:
                    §e/eco <give> <player> <amount> - Allows you to increase player's balance
                    §e/eco <take> <player> <amount> - Allows you to decrease player's balance
                    §e/eco <set> <player> <amount> - Allows you to set a player's balance
                    §e/adminbuyorder <item> <amount> <price> - Allows you to create an unlimited admin buy shop
                    §e/adminsellorder <item> <amount> <price> - Allows you to create an unlimited admin sell shop
                    §e/deleteshop - Allows you to delete all admin and player shops""");
        }
        return true;
    }
}
