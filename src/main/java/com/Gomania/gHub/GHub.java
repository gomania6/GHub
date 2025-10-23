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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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
    private boolean itemEnabled; // <-- ДОБАВЛЕНО

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
        if (hasPlaceholderAPI) getLogger().info("PlaceholderAPI найден! Поддержка плейсхолдеров включена.");

        getLogger().info("GHub включен!");
    }

    @Override
    public void onDisable() {
        lastTeleportTime.clear();
        isFallingIntoVoid.clear();
        getLogger().info("GHub выключен!");
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
            catch (IOException e) { getLogger().warning("Не удалось создать spawn.yml: " + e.getMessage()); }
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
        catch (IOException e) { getLogger().warning("Не удалось сохранить spawn.yml: " + e.getMessage()); }
    }

    private void loadItemSettings() {
        itemEnabled = config.getBoolean("item.enabled", true); // <-- ДОБАВЛЕНО

        if (!itemEnabled) { // <-- ДОБАВЛЕНО
            specialItem = null; // Если предмет выключен, обнуляем его
            return;
        }

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
        // --- ЛОГИКА ПОЛНОСТЬЮ ПЕРЕПИСАНА ---

        // 1. Ищем предмет ВЕЗДЕ в инвентаре
        int existingSlot = -1;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isSpecialItem(item)) {
                existingSlot = i;
                break;
            }
        }

        if (!itemEnabled) {
            // 2. ВЫКЛЮЧЕНО: Если предмет найден, удаляем его.
            if (existingSlot != -1) {
                player.getInventory().clear(existingSlot);
            }
            return; // Выходим, не выдаем
        }

        // 3. ВКЛЮЧЕНО: Проверка, что предмет загружен (на случай ошибки конфига)
        if (specialItem == null) {
            getLogger().warning("specialItem is null, but itemEnabled=true. Проверьте конфиг.");
            return;
        }

        // 4. ВКЛЮЧЕНО: Предмет уже есть у игрока
        if (existingSlot != -1) {
            // 4а. Он в ПРАВИЛЬНОМ слоте. Просто обновляем его (на случай смены конфига)
            if (existingSlot == slot) {
                player.getInventory().setItem(slot, specialItem.clone());
                return;
            }
            // 4b. Он в НЕПРАВИЛЬНОМ слоте. Перемещаем.
            else {
                player.getInventory().clear(existingSlot); // Убираем со старого места
                player.getInventory().setItem(slot, specialItem.clone()); // Ставим в нужный слот
                return;
            }
        }

        // 5. ВКЛЮЧЕНО: Предмета нет. Выдаем в нужный слот.
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

        if (player != null) command = processPlaceholders(player, command);
        else if (command.contains("%player_name%")) {
            sender.sendMessage(ChatColor.RED + "Ошибка: %player_name% нельзя использовать из консоли без игрока!");
            return;
        }

        if (command.startsWith("[console]")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("[console]", "").trim());
        } else if (command.startsWith("[bungee]") && player != null) {
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
            } catch (Exception e) { getLogger().warning("Ошибка PAPI: " + e.getMessage()); }
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
            try { player.setGameMode(GameMode.valueOf(gmName)); }
            catch (IllegalArgumentException e) { getLogger().warning("Неверный режим игры в config.yml: " + gmName); }
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
        giveSpecialItem(p); // Эта функция теперь также удаляет предмет, если он выключен
        applyDefaultGamemode(p);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        if (spawnLocation != null && p.getLocation().getY() <= VOID_HEIGHT
                && p.getVelocity().getY() < -0.5
                && p.getGameMode() != GameMode.CREATIVE
                && p.getGameMode() != GameMode.SPECTATOR) {
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
            Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline()) p.spigot().respawn(); }, 5L);
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
        if (!itemEnabled) return; // <-- ДОБАВЛЕНО
        if (e.getItem() != null && isSpecialItem(e.getItem())) {
            executeFunction(e.getPlayer(), e.getPlayer());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!itemEnabled) return; // <-- ДОБАВЛЕНО
        if (isSpecialItem(e.getCurrentItem()) || isSpecialItem(e.getCursor())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if (!itemEnabled) return; // <-- ДОБАВЛЕНО
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
                giveSpecialItem(p); // Эта функция теперь также удаляет предмет, если он выключен
                applyDefaultGamemode(p);
            }
            sender.sendMessage(getMessage("reload-success", null));
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Только игроки!"); return true; }
            if (!sender.hasPermission("ghub.spawn")) { sender.sendMessage(getMessage("no-permission", (Player) sender)); return true; }
            setSpawnLocation(((Player) sender).getLocation());
            sender.sendMessage(ChatColor.GREEN + "Спавн установлен!");
            return true;
        }

        sender.sendMessage(getMessage("usage", sender instanceof Player ? (Player) sender : null));
        return true;
    }
}