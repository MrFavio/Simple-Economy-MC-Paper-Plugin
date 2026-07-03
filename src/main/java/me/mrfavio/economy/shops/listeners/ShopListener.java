package me.mrfavio.economy.shops.listeners;

import me.mrfavio.economy.Economy;
import me.mrfavio.economy.shops.ShopManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Objects;

public class ShopListener implements Listener {

    private final Economy plugin;
    private final ShopManager shopManager;

    public ShopListener(Economy plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (shopManager.isShop(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis block is part of a shop! Use /deleteshop to remove it.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHopperTransfer(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        if (shopManager.isShopInventory(event.getSource()) || shopManager.isShopInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        if (event.getClickedBlock() == null) return;

        Block clickedBlock = event.getClickedBlock();
        Material blockType = clickedBlock.getType();
        boolean isChest = blockType == Material.CHEST;
        boolean isSign = blockType.name().contains("SIGN");

        if (!isChest && !isSign) return;

        org.bukkit.Location chestLoc = null;
        ShopManager.ShopData shop = null;

        if (isChest) {
            chestLoc = clickedBlock.getLocation();
            shop = shopManager.getShop(chestLoc);
        } else {
            chestLoc = shopManager.getChestLocationFromSign(clickedBlock.getLocation());
            if (chestLoc != null) {
                shop = shopManager.getShop(chestLoc);
            }

            if (shop == null) {
                org.bukkit.block.BlockFace[] faces = {org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST, org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN};
                for (org.bukkit.block.BlockFace face : faces) {
                    Block relative = clickedBlock.getRelative(face);
                    if (relative.getType() == Material.CHEST) {
                        ShopManager.ShopData foundShop = shopManager.getShop(relative.getLocation());
                        if (foundShop != null) {
                            chestLoc = relative.getLocation();
                            shop = foundShop;
                            break;
                        }
                    }
                }
            }
        }

        if (shop == null || chestLoc == null) return;

        Player player = event.getPlayer();

        if (isChest) {
            org.bukkit.Location exactChestLoc = clickedBlock.getLocation();

            if (clickedBlock.getState() instanceof org.bukkit.block.Chest chestState) {
                org.bukkit.inventory.DoubleChestInventory doubleInventory =
                        chestState.getInventory() instanceof org.bukkit.inventory.DoubleChestInventory
                                ? (org.bukkit.inventory.DoubleChestInventory) chestState.getInventory()
                                : null;

                if (doubleInventory != null) {
                    org.bukkit.block.DoubleChest doubleChest = (org.bukkit.block.DoubleChest) doubleInventory.getHolder();
                    if (doubleChest != null) {
                        exactChestLoc = ((org.bukkit.block.Chest) Objects.requireNonNull(doubleChest.getLeftSide())).getLocation();
                    }
                }
            }

            ShopManager.ShopData shoper = shopManager.getShop(exactChestLoc);

            if (shoper != null) {
                if (!shoper.owner.equals(player.getUniqueId()) && !player.isOp()) {
                    event.setCancelled(true);
                    player.sendMessage("§cOnly the shop owner can open this chest!");
                    return;
                }
            }
            return;
        }

        if (isSign) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);

                if (!shop.isAdmin && shop.owner.equals(player.getUniqueId())) {
                    player.sendMessage("§cYou cannot interact with your own shop!");
                    return;
                }

                if (chestLoc.getBlock().getType() != Material.CHEST) {
                    player.sendMessage("§cThe shop chest is missing or broken!");
                    return;
                }

                Chest chest = (Chest) chestLoc.getBlock().getState();
                Inventory chestInv = chest.getInventory();
                Material itemMat = Material.valueOf(shop.itemMaterial);

                if (shop.type.equals("BUY")) {
                    handleBuyOrder(player, shop, chestInv, itemMat);
                } else if (shop.type.equals("SELL")) {
                    handleSellOrder(player, shop, chestInv, itemMat);
                }
            }
        }
    }

    private void handleBuyOrder(Player clicker, ShopManager.ShopData shop, Inventory chestInv, Material mat) {
        ItemStack itemToTake = new ItemStack(mat, shop.amount);

        if (!clicker.getInventory().containsAtLeast(itemToTake, shop.amount)) {
            clicker.sendMessage("§cThis shop wants to buy %dx %s, but you don't have enough!".formatted(shop.amount, mat.name()));
            return;
        }

        if (shop.isAdmin) {
            clicker.getInventory().removeItem(itemToTake);

            plugin.setBalance(clicker.getUniqueId(), plugin.getBalance(clicker.getUniqueId()) + shop.price);
            plugin.updateTabBalance(clicker);

            clicker.sendMessage("§6[Admin] §aYou sold %dx %s for %d$!".formatted(shop.amount, mat.name(), shop.price));
            return;
        }

        HashMap<Integer, ItemStack> leftOver = chestInv.addItem(itemToTake);
        if (!leftOver.isEmpty()) {
            chestInv.removeItem(itemToTake);
            clicker.sendMessage("§cThe shop chest is full!");
            return;
        }

        clicker.getInventory().removeItem(itemToTake);

        plugin.setBalance(shop.owner, plugin.getBalance(shop.owner) - shop.price);
        plugin.setBalance(clicker.getUniqueId(), plugin.getBalance(clicker.getUniqueId()) + shop.price);

        plugin.updateTabBalance(clicker);
        Player ownerPlayer = org.bukkit.Bukkit.getPlayer(shop.owner);
        if (ownerPlayer != null) plugin.updateTabBalance(ownerPlayer);

        clicker.sendMessage("§aYou sold %dx %s for %d$!".formatted(shop.amount, mat.name(), shop.price));
    }

    private void handleSellOrder(Player clicker, ShopManager.ShopData shop, Inventory chestInv, Material mat) {
        if (plugin.getBalance(clicker.getUniqueId()) < shop.price) {
            clicker.sendMessage("§cYou do not have enough money (%d$)!".formatted(shop.price));
            return;
        }

        ItemStack itemToGive = new ItemStack(mat, shop.amount);

        if (shop.isAdmin) {
            HashMap<Integer, ItemStack> leftOver = clicker.getInventory().addItem(itemToGive);
            if (!leftOver.isEmpty()) {
                clicker.sendMessage("§cYour inventory is full!");
                return;
            }

            plugin.setBalance(clicker.getUniqueId(), plugin.getBalance(clicker.getUniqueId()) - shop.price);
            plugin.updateTabBalance(clicker);

            clicker.sendMessage("§6[Admin] §aYou bought %dx %s for %d$!".formatted(shop.amount, mat.name(), shop.price));
            return;
        }

        if (!chestInv.containsAtLeast(itemToGive, shop.amount)) {
            clicker.sendMessage("§cOut of stock! This shop is empty.");
            return;
        }

        chestInv.removeItem(itemToGive);
        HashMap<Integer, ItemStack> leftOver = clicker.getInventory().addItem(itemToGive);

        if (!leftOver.isEmpty()) {
            chestInv.addItem(itemToGive);
            clicker.sendMessage("§cYour inventory is full!");
            return;
        }

        plugin.setBalance(clicker.getUniqueId(), plugin.getBalance(clicker.getUniqueId()) - shop.price);
        plugin.setBalance(shop.owner, plugin.getBalance(shop.owner) + shop.price);

        plugin.updateTabBalance(clicker);
        Player ownerPlayer = org.bukkit.Bukkit.getPlayer(shop.owner);
        if (ownerPlayer != null) plugin.updateTabBalance(ownerPlayer);

        clicker.sendMessage("§aYou bought %dx %s for %d$!".formatted(shop.amount, mat.name(), shop.price));
    }
}