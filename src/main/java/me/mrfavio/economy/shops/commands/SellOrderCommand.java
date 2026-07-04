package me.mrfavio.economy.shops.commands;

import me.mrfavio.economy.shops.ShopManager;

public class SellOrderCommand extends AbstractOrderCommand {

    public SellOrderCommand(ShopManager shopManager) {
        super(shopManager, "SELL", false, "sellorder");
    }

    @Override
    protected String successMessage() {
        return "§aSuccessfully created a selling shop!";
    }
}