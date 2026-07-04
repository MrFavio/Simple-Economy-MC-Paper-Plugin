package me.mrfavio.economy.shops.commands;

import me.mrfavio.economy.shops.ShopManager;

public class AdminSellOrderCommand extends AbstractOrderCommand {

    public AdminSellOrderCommand(ShopManager shopManager) {
        super(shopManager, "SELL", true, "adminsellorder");
    }

    @Override
    protected String successMessage() {
        return "§aSuccessfully created an ADMIN selling shop! (unlimited stock, no payout to owner)";
    }
}