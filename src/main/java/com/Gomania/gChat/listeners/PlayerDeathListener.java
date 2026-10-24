package com.Gomania.gChat.listeners;

import com.Gomania.gChat.GChat;
import com.Gomania.gChat.managers.ConfigManager;
import com.Gomania.gChat.utils.ChatColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Заменяет стандартные сообщения о смерти на кастомные из config.yml.
 */
public class PlayerDeathListener implements Listener {

    private final GChat plugin;
    private final ConfigManager config;

    public PlayerDeathListener(GChat plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!config.isDeathMessagesEnabled()) {
            return;
        }

        Player player = event.getEntity();
        Player killer = player.getKiller(); // Это игрок, нанесший последний удар

        String customDeathMessage = null;

        if (killer != null) {
            // Убийство игроком
            customDeathMessage = config.getDeathMsgPlayerKill()
                    .replace("%player%", player.getName())
                    .replace("%killer%", killer.getName());

        } else {
            EntityDamageEvent lastDamage = player.getLastDamageCause();

            if (lastDamage != null) {
                if (lastDamage instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) lastDamage;
                    Entity damager = damageByEntity.getDamager();

                    if (damager instanceof LivingEntity && !(damager instanceof Player)) {
                        // Убийство мобом
                        String mobName = damager.getCustomName() != null ? damager.getCustomName() : damager.getName();
                        customDeathMessage = config.getDeathMsgMobKill()
                                .replace("%player%", player.getName())
                                .replace("%killer_mob%", mobName);
                    }
                }

                // НОВОЕ: Проверка на специфическую причину смерти (падение, лава, огонь и т.д.)
                if (customDeathMessage == null) {
                    String causeName = lastDamage.getCause().name();
                    String causeMsg = config.getDeathMsgCause(causeName);
                    if (causeMsg != null) {
                        customDeathMessage = causeMsg.replace("%player%", player.getName());
                    }
                }
            }
        }

        // Если причина не найдена (и не настроена в causes)
        if (customDeathMessage == null) {
            customDeathMessage = config.getDeathMsgOther()
                    .replace("%player%", player.getName());
        }

        // Форматируем сообщение
        customDeathMessage = ChatColorUtils.translateColors(customDeathMessage, true, true);

        // Отправляем кастомное сообщение только тем, у кого включена настройка
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getSettingsManager().getSettings(p.getUniqueId()).showDeathMessages) {
                p.sendMessage(customDeathMessage);
            }
        }

        // Отправляем в консоль для логов
        Bukkit.getConsoleSender().sendMessage(customDeathMessage);

        // Скрываем стандартное
        if (config.isHideDefaultDeathMessage()) {
            event.setDeathMessage(null);
        }
    }
}