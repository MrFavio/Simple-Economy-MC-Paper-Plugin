package me.mrfavio.economy.shops.commands;

import me.mrfavio.economy.shops.ShopManager;

public class AdminBuyOrderCommand extends AbstractOrderCommand {

    public AdminBuyOrderCommand(ShopManager shopManager) {
        super(shopManager, "BUY", true, "adminbuyorder");
    }

    @Override
    protected String successMessage() {
        return "§aSuccessfully created an ADMIN buying order shop! (unlimited funds, no owner payout)";
    }
}