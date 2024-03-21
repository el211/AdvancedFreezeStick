package com.afelia.advancedfreezestick;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.GameMode;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedFreezeStick extends JavaPlugin implements Listener {
    private int maxUses;
    private int cooldownSeconds;
    private int customModelData;
    private int freezingTime;
    private Map<Player, Integer> remainingUsesMap;
    private Map<Player, Long> lastUsedTimeMap;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        loadConfig();
        remainingUsesMap = new HashMap<>();
        lastUsedTimeMap = new HashMap<>();
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        maxUses = config.getInt("max_uses", 10);
        cooldownSeconds = config.getInt("cooldown", 5);
        customModelData = config.getInt("custom_model_data", 0);
        freezingTime = config.getInt("freezing_time", 5); // Initialize freezingTime from config
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("givefreezestick")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }
            Player player = (Player) sender;

            // Check if player has permission to give freezing sticks
            if (!player.hasPermission("freezestick.give")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            // Example usage of createFreezingStick method
            int remainingUses = 0; // Initially set to zero uses
            ItemStack stick = createFreezingStick(remainingUses);
            player.getInventory().addItem(stick);
            return true;
        } else if (command.getName().equalsIgnoreCase("giveplayerfreezestick")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /giveplayerfreezestick <player>");
                return false;
            }

            // Check if the sender is the console
            if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                sender.sendMessage(ChatColor.RED + "This command can only be executed from the console.");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found or not online.");
                return true;
            }

            // Check if console has permission to give freezing sticks to other players
            if (!sender.hasPermission("freezestick.giveplayer")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            int remainingUses = 0; // Initially set to zero uses
            ItemStack stick = createFreezingStick(remainingUses);
            target.getInventory().addItem(stick);
            sender.sendMessage(ChatColor.GREEN + "You have given a freezing stick to " + target.getName() + ".");
            return true;
        }
        return false;
    }

    private ItemStack createFreezingStick(int remainingUses) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            // Set display name with color codes
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "Freezing Stick"));

            // Set lore with color codes
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "Right-click to freeze an entity!"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "Remaining Uses: " + remainingUses + "/" + maxUses));
            meta.setLore(lore);

            // Set custom model data
            meta.setCustomModelData(customModelData);

            stick.setItemMeta(meta);
        }
        return stick;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == Material.STICK && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', "Freezing Stick"))) {
                if (checkCooldown(player)) {
                    player.sendMessage("Stick is on cooldown!");
                    return;
                }

                Entity target = event.getRightClicked();
                if (target != null && (target instanceof LivingEntity || target.getType() == EntityType.ARMOR_STAND)) {
                    // Check if the interaction is with a living entity or an armor stand
                    // Ignore item frames and glow item frames
                    if (target.getType() == EntityType.ITEM_FRAME || target.getType() == EntityType.GLOW_ITEM_FRAME) {
                        return;
                    }

                    // Check if there are remaining uses left
                    if (getRemainingUses(player) <= 0) {
                        player.sendMessage(ChatColor.RED + "You have used all the remaining uses of the Freezing Stick!");
                        return;
                    }
                    // If there are remaining uses, freeze the entity
                    freezeEntity(target);
                    updateRemainingUses(player);
                    updateLastUsedTime(player);
                }
            }
        }
    }


    private void freezeEntity(Entity entity) {
        // Implement freezing logic for the targeted entity
        // For example, you can disable their movement or actions temporarily
        // Here's a simple example:
        if (entity instanceof LivingEntity) {
            ((LivingEntity) entity).setAI(false); // Disable AI to freeze the entity
            Bukkit.getScheduler().runTaskLater(this, () -> {
                ((LivingEntity) entity).setAI(true);
            }, (long) freezingTime * 20);
        }
    }

    private boolean checkCooldown(Player player) {
        if (!lastUsedTimeMap.containsKey(player)) {
            return false;
        }
        long lastUsedTime = lastUsedTimeMap.get(player);
        long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
        return (currentTime - lastUsedTime) < cooldownSeconds;
    }

    private void updateRemainingUses(Player player) {
        int remainingUses = getRemainingUses(player);
        if (remainingUses > 0) {
            remainingUsesMap.put(player, remainingUses - 1);
            // Update the lore of the freezing stick
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.getType() == Material.STICK && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore.size() >= 2) {
                        lore.set(1, ChatColor.translateAlternateColorCodes('&', "Remaining Uses: " + (remainingUses - 1) + "/" + maxUses));
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
            }
        }
    }


    private int getRemainingUses(Player player) {
        return remainingUsesMap.getOrDefault(player, maxUses);
    }

    private void updateLastUsedTime(Player player) {
        lastUsedTimeMap.put(player, System.currentTimeMillis() / 1000); // Convert to seconds
    }
}
