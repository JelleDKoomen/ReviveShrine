/*
 * Plugin: ReviveShrine
 * Server: Paper 1.20.4 (werkt ook op 1.21.x zodra Paper release ondersteunt)
 * Features:
 * - Configureerbare revive prijs
 * - Revive via kist op ingestelde locatie
 * - Diamond block visualisatie voor aantal revives
 * - Commands voor setup
 * - Automatische doodregistratie
 * - Persistentie van dode spelers
 */

package com.Jelle;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ReviveShrine extends JavaPlugin implements Listener {

    private Location reviveChestLocation;
    private Location counterLocation;
    private int totalRevives = 0;
    private final Set<UUID> deadPlayers = new HashSet<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLocationsFromConfig();
        loadDeadPlayers();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ReviveShrine enabled.");
    }

    @Override
    public void onDisable() {
        saveLocationsToConfig();
        saveDeadPlayers();
        getLogger().info("ReviveShrine disabled.");
    }

    private void loadLocationsFromConfig() {
        FileConfiguration config = getConfig();
        if (config.contains("revive-chest")) {
            reviveChestLocation = (Location) config.get("revive-chest");
        }
        if (config.contains("counter-block")) {
            counterLocation = (Location) config.get("counter-block");
        }
        totalRevives = config.getInt("total-revives", 0);
    }

    private void saveLocationsToConfig() {
        FileConfiguration config = getConfig();
        if (reviveChestLocation != null)
            config.set("revive-chest", reviveChestLocation);
        if (counterLocation != null)
            config.set("counter-block", counterLocation);
        config.set("total-revives", totalRevives);
        saveConfig();
    }

    private void loadDeadPlayers() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                if (dataFile.createNewFile()) {
                    getLogger().info("data.yml succesvol aangemaakt.");
                }
            } catch (IOException e) {
                getLogger().severe("Fout bij laden van data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        List<String> list = dataConfig.getStringList("dead-players");
        for (String uuid : list) {
            deadPlayers.add(UUID.fromString(uuid));
        }
    }

    private void saveDeadPlayers() {
        List<String> list = new ArrayList<>();
        for (UUID uuid : deadPlayers) {
            list.add(uuid.toString());
        }
        dataConfig.set("dead-players", list);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Fout bij laden opslaan van de spelers: " + e.getMessage());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (reviveChestLocation == null) return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        Block block = reviveChestLocation.getBlock();
        if (!(block.getState() instanceof Chest chest)) return;
        if (!chest.getInventory().equals(inv)) return;

        int required = getConfig().getInt("revive-price.DIAMOND", 64);
        int count = countItems(inv);

        if (count >= required) {
            removeItems(inv, required);
            reviveRandomDeadPlayer(player);
            updateCounterBlock();
            player.sendMessage("§aRevive uitgevoerd! Een speler is teruggebracht.");
            getLogger().info("ReviveShrine succesvol aangemaakt. ");
        }
    }

    private void reviveRandomDeadPlayer(Player trigger) {
        if (deadPlayers.isEmpty()) {
            trigger.sendMessage("§cEr zijn geen dode spelers om te reviven.");
            return;
        }
        UUID revivedId = deadPlayers.iterator().next();
        deadPlayers.remove(revivedId);
        Player revived = Bukkit.getPlayer(revivedId);
        if (revived != null) {
            revived.setGameMode(GameMode.SURVIVAL);
            revived.setHealth(20.0);
            revived.setFoodLevel(20);
            revived.teleport(trigger.getLocation());
            revived.sendMessage("§aJe bent revived door een offer!");
        }
        totalRevives++;
    }

    private void updateCounterBlock() {
        if (counterLocation == null) return;

        int index = totalRevives - 1;
        int layer = index / 25;
        int indexInLayer = index % 25;
        int xOffset = indexInLayer % 5;
        int zOffset = indexInLayer / 5;

        Location blockLocation = counterLocation.clone().add(xOffset, layer, zOffset);
        blockLocation.getBlock().setType(Material.DIAMOND_BLOCK);
    }

    private int countItems(Inventory inv) {
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Inventory inv, int amount) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == Material.DIAMOND) {
                int toRemove = Math.min(item.getAmount(), amount);
                item.setAmount(item.getAmount() - toRemove);
                amount -= toRemove;
                if (amount <= 0) break;
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        return switch (cmd.getName().toLowerCase()) {
            case "setrevivechest" -> {
                handleSetReviveChest(player);
                yield true;
            }
            case "setrevivecounter" -> {
                handleSetReviveCounter(player);
                yield true;
            }
            case "revivelist" -> {
                handleReviveList(sender);
                yield true;
            }
            case "setrevivecount" -> {
                handleSetReviveCount(sender, args);
                yield true;
            }
            default -> false;
        };
    }

    private void handleSetReviveChest(Player player) {
        reviveChestLocation = player.getLocation().getBlock().getLocation();
        player.sendMessage("§aRevive chest locatie ingesteld.");
        getLogger().info("Revive chest locatie ingesteld door " + player.getName() + ": " + reviveChestLocation);
    }

    private void handleSetReviveCounter(Player player) {
        counterLocation = player.getLocation().getBlock().getLocation();
        player.sendMessage("§aRevive counter locatie ingesteld.");
        getLogger().info("Revive counter locatie ingesteld door " + player.getName() + ": " + counterLocation);
    }

    private void handleReviveList(CommandSender sender) {
        sender.sendMessage("§eDode spelers:");
        if (deadPlayers.isEmpty()) {
            sender.sendMessage("§cGeen dode spelers.");
            return;
        }
        for (UUID id : deadPlayers) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(id);
            sender.sendMessage("- " + (p.getName() != null ? p.getName() : id.toString()));
        }
    }

    private void handleSetReviveCount(CommandSender sender, String[] args) {
        if (!sender.hasPermission("revivecount.set")) {
            sender.sendMessage("§cYou do not have permission to set the revive count.");
            return;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /setrevivecount <count>");
            return;
        }

        try {
            int newCount = Integer.parseInt(args[0]);
            if (newCount < 0) {
                sender.sendMessage("§cThe revive count cannot be negative.");
                return;
            }

            totalRevives = newCount;
            updateCounterBlock();
            sender.sendMessage("§aTotal revive count set to " + newCount + " and blocks updated.");
            getLogger().info("Total revive count set to " + newCount + " by " + sender.getName());
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number. Please enter a valid integer.");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        registerDeath(player.getUniqueId());
        player.sendMessage("§cJe bent gestorven! Je moet gerevived worden door anderen.");
    }

    public void registerDeath(UUID playerId) {
        deadPlayers.add(playerId);
    }
}
