package me.mrfavio.economy.shops;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ShopManager {

    private final JavaPlugin plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<Location, ShopData> shops = new HashMap<>();
    private final Map<Location, org.bukkit.entity.ItemDisplay> displays = new HashMap<>();
    private final org.bukkit.NamespacedKey displayKey;
    private double rotationAngle = 0;

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.displayKey = new org.bukkit.NamespacedKey(plugin, "shop_item_display");
        this.file = new File(plugin.getDataFolder(), "shops.yml");
        if (!file.exists()) {
            if (!plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create the plugin data folder!");
            }
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("shops.yml unexpectedly already existed!");
                }
            } catch (IOException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to make shops.yml!", e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        loadShops();
        respawnAllDisplays();
        startRotationTask();
    }

    public static class ShopData {
        public UUID owner;
        public String type;
        public org.bukkit.inventory.ItemStack item;
        public int amount;
        public int price;
        public Location signLocation;
        public boolean isAdmin;

        public ShopData(UUID owner, String type, org.bukkit.inventory.ItemStack item, int amount, int price, Location signLocation, boolean isAdmin) {
            this.owner = owner;
            this.type = type;
            this.item = item;
            this.amount = amount;
            this.price = price;
            this.signLocation = signLocation;
            this.isAdmin = isAdmin;
        }
    }

    public void createShop(Location chestLoc, UUID owner, String type, org.bukkit.inventory.ItemStack item, int amount, int price, Location signLoc, boolean isAdmin) {
        ShopData data = new ShopData(owner, type, item, amount, price, signLoc, isAdmin);

        shops.put(chestLoc, data);

        saveShopToConfig(chestLoc, data);
        spawnFloatingItem(chestLoc, data.item);
    }

    public void removeShop(Location chestLoc) {
        shops.remove(chestLoc);

        org.bukkit.entity.ItemDisplay display = displays.remove(chestLoc);
        if (display != null && !display.isDead()) {
            display.remove();
        }

        String key = locToString(chestLoc);
        config.set("shops." + key, null);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to remove shop from shops.yml!", e);
        }
    }

    public ShopData getShop(Location loc) {
        return shops.get(loc);
    }

    public boolean isShop(Location loc) {
        return shops.containsKey(loc) || shops.values().stream().anyMatch(s -> loc.equals(s.signLocation));
    }

    public boolean isShopChest(Location loc) {
        return shops.containsKey(loc);
    }

    public boolean isShopInventory(org.bukkit.inventory.Inventory inventory) {
        if (inventory == null) return false;
        org.bukkit.inventory.InventoryHolder holder = inventory.getHolder();

        if (holder instanceof org.bukkit.block.DoubleChest doubleChest) {
            org.bukkit.block.Chest left = (org.bukkit.block.Chest) doubleChest.getLeftSide();
            org.bukkit.block.Chest right = (org.bukkit.block.Chest) doubleChest.getRightSide();
            boolean leftIsShop = left != null && isShopChest(left.getLocation());
            boolean rightIsShop = right != null && isShopChest(right.getLocation());
            return leftIsShop || rightIsShop;
        }

        if (holder instanceof org.bukkit.block.Chest chest) {
            return isShopChest(chest.getLocation());
        }

        return false;
    }

    public Location getChestLocationFromSign(Location signLoc) {
        String searchSignKey = locToString(signLoc);

        if (config.getConfigurationSection("shops") == null) return null;

        for (String chestKey : Objects.requireNonNull(config.getConfigurationSection("shops")).getKeys(false)) {
            String savedSignKey = config.getString("shops." + chestKey + ".sign");
            if (searchSignKey.equals(savedSignKey)) {
                return stringToLoc(chestKey);
            }
        }
        return null;
    }

    private void saveShopToConfig(Location loc, ShopData data) {
        String key = locToString(loc);

        config.set("shops." + key + ".owner", data.owner.toString());
        config.set("shops." + key + ".type", data.type);
        config.set("shops." + key + ".item", data.item);
        config.set("shops." + key + ".amount", data.amount);
        config.set("shops." + key + ".price", data.price);
        config.set("shops." + key + ".admin", data.isAdmin);

        config.set("shops." + key + ".sign", locToString(data.signLocation));

        try {
            config.save(file);
        } catch (java.io.IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save shop data to shops.yml!", e);
        }
    }

    private void loadShops() {
        if (!config.contains("shops")) return;
        for (String key : Objects.requireNonNull(config.getConfigurationSection("shops")).getKeys(false)) {
            Location chestLoc = stringToLoc(key);
            UUID owner = UUID.fromString(Objects.requireNonNull(config.getString("shops." + key + ".owner")));
            String type = config.getString("shops." + key + ".type");
            org.bukkit.inventory.ItemStack item = config.getItemStack("shops." + key + ".item");
            int amount = config.getInt("shops." + key + ".amount");
            int price = config.getInt("shops." + key + ".price");
            boolean isAdmin = config.getBoolean("shops." + key + ".admin", false);
            Location signLoc = stringToLoc(Objects.requireNonNull(config.getString("shops." + key + ".sign")));

            shops.put(chestLoc, new ShopData(owner, type, item, amount, price, signLoc, isAdmin));
        }
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLoc(String str) {
        String[] parts = str.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    public void resyncChunkDisplays(org.bukkit.Chunk chunk) {
        for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
            if (!(entity instanceof org.bukkit.entity.ItemDisplay display)) continue;
            if (!display.getPersistentDataContainer().has(displayKey, org.bukkit.persistence.PersistentDataType.BYTE)) continue;
            if (!displays.containsValue(display)) {
                display.remove();
            }
        }

        for (Map.Entry<Location, ShopData> entry : shops.entrySet()) {
            Location chestLoc = entry.getKey();
            if (!chestLoc.getWorld().equals(chunk.getWorld())) continue;
            if ((chestLoc.getBlockX() >> 4) != chunk.getX() || (chestLoc.getBlockZ() >> 4) != chunk.getZ()) continue;

            org.bukkit.entity.ItemDisplay tracked = displays.get(chestLoc);
            if (tracked != null && !tracked.isDead() && tracked.isValid()) continue;

            removeStaleDisplays(chestLoc);
            spawnFloatingItem(chestLoc, entry.getValue().item);
        }
    }

    private void spawnFloatingItem(Location chestLoc, org.bukkit.inventory.ItemStack item) {
        Location displayLoc = new Location(chestLoc.getWorld(), chestLoc.getBlockX() + 0.5, chestLoc.getBlockY() + 1.3, chestLoc.getBlockZ() + 0.5);

        org.bukkit.entity.ItemDisplay display = chestLoc.getWorld().spawn(displayLoc, org.bukkit.entity.ItemDisplay.class, entity -> {
            entity.setItemStack(item.clone());
            entity.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            entity.setInterpolationDuration(2);
            entity.setInterpolationDelay(0);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(displayKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        });

        displays.put(chestLoc, display);
    }

    private void removeStaleDisplays(Location chestLoc) {
        Location displayLoc = new Location(chestLoc.getWorld(), chestLoc.getBlockX() + 0.5, chestLoc.getBlockY() + 1.3, chestLoc.getBlockZ() + 0.5);

        for (org.bukkit.entity.Entity nearby : chestLoc.getWorld().getNearbyEntities(displayLoc, 0.6, 0.6, 0.6)) {
            if (nearby instanceof org.bukkit.entity.ItemDisplay && nearby.getPersistentDataContainer().has(displayKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                nearby.remove();
            }
        }
    }

    private void respawnAllDisplays() {
        for (Map.Entry<Location, ShopData> entry : shops.entrySet()) {
            Location chestLoc = entry.getKey();
            chestLoc.getChunk().load();
            removeStaleDisplays(chestLoc);
            spawnFloatingItem(chestLoc, entry.getValue().item);
        }
    }

    private void startRotationTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            rotationAngle += Math.toRadians(6);
            if (rotationAngle >= Math.PI * 2) rotationAngle -= Math.PI * 2;

            org.bukkit.util.Transformation transformation = new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(0f, 0f, 0f),
                    new org.joml.Quaternionf().rotateY((float) rotationAngle),
                    new org.joml.Vector3f(0.5f, 0.5f, 0.5f),
                    new org.joml.Quaternionf()
            );

            java.util.Iterator<org.bukkit.entity.ItemDisplay> iterator = displays.values().iterator();
            while (iterator.hasNext()) {
                org.bukkit.entity.ItemDisplay display = iterator.next();
                if (display == null || display.isDead()) {
                    iterator.remove();
                    continue;
                }
                display.setTransformation(transformation);
            }
        }, 0L, 2L);
    }

    public static String getItemDisplayName(org.bukkit.inventory.ItemStack item) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(Objects.requireNonNull(meta.displayName()));
        }

        if (item.getType() == Material.ENCHANTED_BOOK && meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta && !bookMeta.getStoredEnchants().isEmpty()) {
            Map.Entry<org.bukkit.enchantments.Enchantment, Integer> first = bookMeta.getStoredEnchants().entrySet().iterator().next();
            return first.getKey().getKey().getKey().replace('_', ' ') + " " + first.getValue();
        }

        if (!item.getEnchantments().isEmpty()) {
            Map.Entry<org.bukkit.enchantments.Enchantment, Integer> first = item.getEnchantments().entrySet().iterator().next();
            return item.getType().name() + " (" + first.getKey().getKey().getKey().replace('_', ' ') + " " + first.getValue() + ")";
        }

        return item.getType().name();
    }

    public Sign createShopSign(Player player, Block targetBlock, String type, org.bukkit.inventory.ItemStack item, int amount, int price, boolean isAdmin) {
        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            player.sendMessage("§cYou must be looking at a chest (max 5 blocks away)!");
            return null;
        }

        if (isShop(targetBlock.getLocation())) {
            player.sendMessage("§cThis chest is already registered as a shop!");
            return null;
        }

        org.bukkit.block.data.Directional directional = (org.bukkit.block.data.Directional) targetBlock.getBlockData();
        org.bukkit.block.BlockFace front = directional.getFacing();
        org.bukkit.block.Block signBlock = targetBlock.getRelative(front);

        if (signBlock.getType() != Material.AIR) {
            player.sendMessage("§cThe block in front of the chest must be empty to place a sign!");
            return null;
        }

        signBlock.setType(Material.OAK_WALL_SIGN);
        org.bukkit.block.data.Directional signData = (org.bukkit.block.data.Directional) signBlock.getBlockData();
        signData.setFacing(front);
        signBlock.setBlockData(signData);

        org.bukkit.block.Sign sign = (org.bukkit.block.Sign) signBlock.getState();
        org.bukkit.block.sign.SignSide frontSide = sign.getSide(org.bukkit.block.sign.Side.FRONT);

        String itemName = getItemDisplayName(item);

        if (isAdmin) {
            String actionTag = type.equals("BUY") ? "§6[Buying]" : "§6[Selling]";
            frontSide.line(0, net.kyori.adventure.text.Component.text("§6§l★ ADMIN ★"));
            frontSide.line(1, net.kyori.adventure.text.Component.text(actionTag));
            frontSide.line(2, net.kyori.adventure.text.Component.text("§f" + amount + "x " + itemName));
            frontSide.line(3, net.kyori.adventure.text.Component.text("§ffor §6" + price + "$"));
        } else {
            String actionTag = type.equals("BUY") ? "§1[Buying]" : "§4[Selling]";
            frontSide.line(0, net.kyori.adventure.text.Component.text(actionTag));
            frontSide.line(1, net.kyori.adventure.text.Component.text(amount + "x " + itemName));
            frontSide.line(2, net.kyori.adventure.text.Component.text("for"));
            frontSide.line(3, net.kyori.adventure.text.Component.text(price + "$"));
        }
        sign.update();

        return sign;
    }
}