package me.mrfavio.economy;

import me.mrfavio.economy.commands.BalanceCommand;
import me.mrfavio.economy.commands.EconomyCommand;
import me.mrfavio.economy.commands.InfoCommand;
import me.mrfavio.economy.commands.PayCommand;
import me.mrfavio.economy.listeners.PlayerJoinListener;
import me.mrfavio.economy.listeners.PlayerQuitListener;
import me.mrfavio.economy.shops.ShopManager;
import me.mrfavio.economy.shops.commands.BuyOrderCommand;
import me.mrfavio.economy.shops.commands.AdminBuyOrderCommand;
import me.mrfavio.economy.shops.commands.AdminSellOrderCommand;
import me.mrfavio.economy.shops.commands.DeleteShopCommand;
import me.mrfavio.economy.shops.commands.SellOrderCommand;
import me.mrfavio.economy.shops.listeners.ShopChunkListener;
import me.mrfavio.economy.shops.listeners.ShopListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class Economy extends JavaPlugin {

    private final HashMap<UUID, Double> balances = new HashMap<>();
    private Objective ecoObjective;

    private File balancesFile;
    private FileConfiguration balancesConfig;

    @Override
    public void onEnable() {
        createBalancesConfig();
        loadBalancesData();

        Objects.requireNonNull(getCommand("bal")).setExecutor(new BalanceCommand(this));
        Objects.requireNonNull(getCommand("pay")).setExecutor(new PayCommand(this));
        Objects.requireNonNull(getCommand("eco")).setExecutor(new EconomyCommand(this));

        Objects.requireNonNull(getCommand("info")).setExecutor(new InfoCommand());

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);

        setupScoreboard();

        ShopManager shopManager = new ShopManager(this);

        Objects.requireNonNull(getCommand("buyorder")).setExecutor(new BuyOrderCommand(shopManager));
        Objects.requireNonNull(getCommand("sellorder")).setExecutor(new SellOrderCommand(shopManager));
        Objects.requireNonNull(getCommand("deleteshop")).setExecutor(new DeleteShopCommand(shopManager));
        Objects.requireNonNull(getCommand("adminbuyorder")).setExecutor(new AdminBuyOrderCommand(shopManager));
        Objects.requireNonNull(getCommand("adminsellorder")).setExecutor(new AdminSellOrderCommand(shopManager));

        getServer().getPluginManager().registerEvents(new ShopListener(this, shopManager), this);
        getServer().getPluginManager().registerEvents(new ShopChunkListener(shopManager), this);

        startPassiveIncomeTask();
    }

    @Override
    public void onDisable() {
        saveBalancesData();
    }

    private void createBalancesConfig() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                getLogger().warning("Could not create the plugin data folder!");
            }
        }

        balancesFile = new File(dataFolder, "balances.yml");

        if (!balancesFile.exists()) {
            try {
                if (!balancesFile.createNewFile()) {
                    getLogger().warning("balances.yml unexpectedly already existed!");
                } else {
                    getLogger().info("Successfully created balances.yml!");
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create balances.yml!", e);
            }
        }
        balancesConfig = YamlConfiguration.loadConfiguration(balancesFile);
    }

    private void loadBalancesData() {
        if (balancesConfig == null) return;

        for (String key : balancesConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            double balance = balancesConfig.getDouble(key);
            balances.put(uuid, balance);
        }
        getLogger().info("Loaded balances for " + balances.size() + "players.");
    }

    public void saveBalancesData() {
        if (balancesConfig == null || balancesFile == null) return;

        for (String key : balancesConfig.getKeys(false)) {
            balancesConfig.set(key, null);
        }

        for (UUID uuid : balances.keySet()) {
            balancesConfig.set(uuid.toString(), balances.get(uuid));
        }

        try {
            balancesConfig.save(balancesFile);
            getLogger().info("Successfully saved balances to balances.yml!");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save data to balances.yml!", e);
        }
    }

    private void setupScoreboard() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        scoreboard.clearSlot(DisplaySlot.PLAYER_LIST);

        ecoObjective = scoreboard.getObjective("economy_tab");

        if (ecoObjective == null) {
            Component displayName = Component.text("$").color(NamedTextColor.DARK_GREEN);

            ecoObjective = scoreboard.registerNewObjective("economy_tab", Criteria.DUMMY, displayName);
        }
    }

    public boolean hasAccount(UUID uuid) {
        return balances.containsKey(uuid);
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            updateTabBalance(player);
        }

        saveBalancesData();
    }

    public void updateTabBalance(Player player) {
        int rawAmount = (int) getBalance(player.getUniqueId());

        Score score = ecoObjective.getScore(player.getName());
        score.setScore(rawAmount);

        String formattedMoney = formatAmount(rawAmount);

        String formattedName = "%s §a%s§2$".formatted(player.getName(), formattedMoney);
        Component fullTabName = Component.text(formattedName);
        player.playerListName(fullTabName);
    }

    private String formatAmount(int money) {
        if (money < 1000) {
            return String.valueOf(money);
        }

        if (money >= 1_000_000_000) {
            double rounded = Math.floor((money / 1_000_000_000.0) * 10.0) / 10.0;
            return (rounded % 1 == 0) ? String.format("%.0fB", rounded) : String.format("%.1fB", rounded);
        }
        if (money >= 1_000_000) {
            double rounded = Math.floor((money / 1_000_000.0) * 10.0) / 10.0;
            return (rounded % 1 == 0) ? String.format("%.0fM", rounded) : String.format("%.1fM", rounded);
        }
        double rounded = Math.floor((money / 1000.0) * 10.0) / 10.0;
        return (rounded % 1 == 0) ? String.format("%.0fK", rounded) : String.format("%.1fK", rounded);
    }

    private BukkitTask incomeTask;

    private void startPassiveIncomeTask() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        boolean doPassiveIncome = config.getBoolean("enabled", true);

        if (incomeTask != null) {
            incomeTask.cancel();
            incomeTask = null;
        }

        if (!doPassiveIncome) {
            getLogger().info("Passive income is disabled in config.yml.");
            return;
        }

        long intervalValue = config.getLong("passive-income.interval-value", 1);
        String intervalUnit = config.getString("passive-income.interval-unit", "HOURS");
        double incomeAmount = config.getDouble("passive-income.amount", 2.0);

        long seconds = switch (intervalUnit.toUpperCase()) {
            case "MINUTES" -> intervalValue * 60;
            case "SECONDS" -> intervalValue;
            default -> intervalValue * 60 * 60;
        };
        long ticks = seconds * 20L;

        incomeTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                double current = getBalance(player.getUniqueId());
                setBalance(player.getUniqueId(), current + incomeAmount);
                player.sendMessage("§aYou received §e" + incomeAmount + "$ §afor playing on server!");
            }
        }, ticks, ticks);
    }
}