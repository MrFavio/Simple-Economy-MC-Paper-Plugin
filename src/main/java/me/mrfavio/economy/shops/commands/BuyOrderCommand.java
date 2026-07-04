package me.mrfavio.economy.shops.commands;

import me.mrfavio.economy.shops.ShopManager;

public class BuyOrderCommand extends AbstractOrderCommand {

    public BuyOrderCommand(ShopManager shopManager) {
        super(shopManager, "BUY", false, "buyorder");
    }

    @Override
    protected String successMessage() {
        return "§aSuccessfully created a buying order shop!";
    }
}