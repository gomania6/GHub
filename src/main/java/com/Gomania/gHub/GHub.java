package com.Gomania.gHub;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.GameMode;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class GHub extends JavaPlugin implements Listener, PluginMessageListener {

    private FileConfiguration config;
    private FileConfiguration spawnConfig;
    private ItemStack specialItem;
    private int slot;
    private String function;
    private NamespacedKey itemKey;
    private Location spawnLocation;
    private final int VOID_HEIGHT = -60;
    private final Map<UUID, Long> lastTeleportTime = new HashMap<>();
    private final Map<UUID, Boolean> isFallingIntoVoid = new HashMap<>();

    private boolean hasPlaceholderAPI = false;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        itemKey = new NamespacedKey(this, "gHub_item");

        loadConfig();
        loadSpawnConfig();

        getServer().getPluginManager().registerEvents(this, this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        hasPlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (hasPlaceholderAPI) getLogger().info("PlaceholderAPI found! Placeholders supported.");

        getLogger().info("GHub enabled!");
    }

    @Override
    public void onDisable() {
        lastTeleportTime.clear();
        isFallingIntoVoid.clear();
        getLogger().info("GHub disabled!");
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) saveResource("config.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);
        loadItemSettings();
    }

    private void loadSpawnConfig() {
        File spawnFile = new File(getDataFolder(), "spawn.yml");
        if (!spawnFile.exists()) {
            try { spawnFile.createNewFile(); }
            catch (IOException e) { getLogger().warning("Failed to create spawn.yml: " + e.getMessage()); }
        }
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);

        if (spawnConfig.contains("spawn")) {
            World world = Bukkit.getWorld(spawnConfig.getString("spawn.world"));
            double x = spawnConfig.getDouble("spawn.x");
            double y = spawnConfig.getDouble("spawn.y");
            double z = spawnConfig.getDouble("spawn.z");
            float yaw = (float) spawnConfig.getDouble("spawn.yaw");
            float pitch = (float) spawnConfig.getDouble("spawn.pitch");

            if (world != null) spawnLocation = new Location(world, x, y, z, yaw, pitch);
        }
    }

    private void saveSpawnConfig() {
        try { spawnConfig.save(new File(getDataFolder(), "spawn.yml")); }
        catch (IOException e) { getLogger().warning("Failed to save spawn.yml: " + e.getMessage()); }
    }

    private void loadItemSettings() {
        Material material;
        try { material = Material.valueOf(config.getString("item.material", "COMPASS").toUpperCase()); }
        catch (IllegalArgumentException e) { material = Material.COMPASS; }

        slot = Math.max(0, Math.min(8, config.getInt("item.slot", 0)));
        function = config.getString("item.function", "");

        specialItem = new ItemStack(material, 1);
        ItemMeta meta = specialItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("item.name", "&6Server Selector")));
            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("item.lore")) lore.add(ChatColor.translateAlternateColorCodes('&', line));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
            specialItem.setItemMeta(meta);
        }
    }

    public void giveSpecialItem(Player player) {
        for (int i = 0; i < 9; i++) {
            if (isSpecialItem(player.getInventory().getItem(i))) return;
        }
        player.getInventory().setItem(slot, specialItem.clone());
    }

    public boolean isSpecialItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    private void executeFunction(CommandSender sender, Player player) {
        if (function == null || function.isEmpty()) return;

        String command = function;

        if (player != null) {
            command = processPlaceholders(player, command);
        } else {
            if (command.contains("%player_name%")) {
                sender.sendMessage(ChatColor.RED + "Ошибка: %player_name% не может быть использован из консоли без игрока!");
                return;
            }
        }

        if (command.startsWith("[console]")) {
            command = command.replace("[console]", "").trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        else if (command.startsWith("[bungee]") && player != null) {
            String server = command.replace("[bungee]", "").trim();
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        }
    }

    private String processPlaceholders(Player player, String text) {
        if (player != null) text = text.replace("%player_name%", player.getName());
        if (hasPlaceholderAPI && player != null) {
            try {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Method m = papi.getMethod("setPlaceholders", Player.class, String.class);
                Object result = m.invoke(null, player, text);
                if (result instanceof String) text = (String) result;
            } catch (Exception e) { getLogger().warning("PAPI placeholder failed: " + e.getMessage()); }
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String getMessage(String path, Player player) {
        String msg = config.getString("messages." + path, "&cMessage not found: " + path);
        return processPlaceholders(player, msg);
    }

    private void setSpawnLocation(Location loc) {
        spawnLocation = loc;
        spawnConfig.set("spawn.world", loc.getWorld().getName());
        spawnConfig.set("spawn.x", loc.getX());
        spawnConfig.set("spawn.y", loc.getY());
        spawnConfig.set("spawn.z", loc.getZ());
        spawnConfig.set("spawn.yaw", loc.getYaw());
        spawnConfig.set("spawn.pitch", loc.getPitch());
        saveSpawnConfig();
    }

    private void applyDefaultGamemode(Player player) {
        if (config.getBoolean("gamemode.enabled", false)) {
            String gmName = config.getString("gamemode.default", "ADVENTURE").toUpperCase();
            try {
                player.setGameMode(GameMode.valueOf(gmName));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Неверный режим игры в config.yml: " + gmName);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (spawnLocation != null) {
            p.teleport(spawnLocation);
            if (config.getBoolean("messages.welcome-spawn-enabled", true))
                p.sendMessage(getMessage("welcome-spawn", p));
        }

        giveSpecialItem(p);
        applyDefaultGamemode(p);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (spawnLocation != null && p.getLocation().getY() <= VOID_HEIGHT && p.getVelocity().getY() < -0.5) {
            long now = System.currentTimeMillis();
            if (now - lastTeleportTime.getOrDefault(id, 0L) > 3000) {
                isFallingIntoVoid.put(id, true);
                p.teleport(spawnLocation);
                p.sendMessage(ChatColor.RED + "Вы упали в пустоту! Телепортация на спавн.");
                lastTeleportTime.put(id, now);
            }
        } else isFallingIntoVoid.remove(id);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (spawnLocation != null && !isFallingIntoVoid.getOrDefault(p.getUniqueId(), false)) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (p.isOnline()) p.spigot().respawn();
            }, 5L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (spawnLocation != null && !isFallingIntoVoid.getOrDefault(e.getPlayer().getUniqueId(), false)) {
            e.setRespawnLocation(spawnLocation);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getItem() != null && isSpecialItem(e.getItem())) {
            executeFunction(e.getPlayer(), e.getPlayer());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (isSpecialItem(e.getCurrentItem()) || isSpecialItem(e.getCursor())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if (isSpecialItem(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(getMessage("cannot-drop", e.getPlayer()));
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("ghub")) return false;

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/ghub reload - Перезагрузить конфиг");
            sender.sendMessage(ChatColor.YELLOW + "/ghub spawn - Установить спавн");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ghub.reload")) { sender.sendMessage(getMessage("no-permission", null)); return true; }
            loadConfig();
            for (Player p : Bukkit.getOnlinePlayers()) {
                giveSpecialItem(p);
                applyDefaultGamemode(p);
            }
            sender.sendMessage(getMessage("reload-success", null));
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Only players!"); return true; }
            if (!sender.hasPermission("ghub.spawn")) { sender.sendMessage(getMessage("no-permission", (Player) sender)); return true; }
            setSpawnLocation(((Player) sender).getLocation());
            sender.sendMessage(ChatColor.GREEN + "Spawn location set!");
            return true;
        }

        sender.sendMessage(getMessage("usage", sender instanceof Player ? (Player) sender : null));
        return true;
    }
}
