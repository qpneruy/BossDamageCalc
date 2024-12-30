package org.qpneruy.bossDamageCalc.events;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.qpneruy.bossDamageCalc.BossDamageCalc;
import org.qpneruy.bossDamageCalc.data.DamageMetaData;
import org.qpneruy.bossDamageCalc.data.ModData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EventListener implements Listener {
    private static final int MAX_REWARD_RANK = 10;
    private final Map<String, Map<UUID, DamageMetaData>> damageData;
    private final BossDamageCalc plugin;
    private final Logger logger;

    public EventListener(BossDamageCalc plugin) {
        this.plugin = plugin;
        this.damageData = new ConcurrentHashMap<>();
        this.logger = plugin.getLogger();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!MythicBukkit.inst().getAPIHelper().isMythicMob(event.getEntity())) {
            return;
        }

        ActiveMob mythicMob = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(event.getEntity());
        String mobTypeId = mythicMob.getMobType();
        Map<UUID, DamageMetaData> mobDamageData = damageData.get(mobTypeId);

        if (mobDamageData == null || mobDamageData.isEmpty()) {
            return;
        }

        processRewards(mythicMob, mobDamageData);
        damageData.remove(mobTypeId);
    }

    private void processRewards(ActiveMob mythicMob, Map<UUID, DamageMetaData> mobDamageData) {
        ModData modData = plugin.data.getModData(mythicMob.getMobType());
        if (modData == null) {
            logger.warning("No ModData found for mob type: " + mythicMob.getMobType());
            return;
        }

        List<DamageMetaData> sortedDamagers = sortByTotalDamage(mobDamageData);
        Map<Integer, List<String>> rewards = modData.getRewards();
        displayLeaderboard(mythicMob, sortedDamagers);

        for (int rank = 0; rank < Math.min(sortedDamagers.size(), MAX_REWARD_RANK); rank++) {
            DamageMetaData damageMetaData = sortedDamagers.get(rank);
            Player player = damageMetaData.getPlayer();

            if (player == null || !player.isOnline()) {
                continue;
            }

            distributeRewards(player, damageMetaData, mythicMob, rewards.get(rank + 1));
        }
    }

    private void displayLeaderboard(ActiveMob mythicMob, List<DamageMetaData> sortedDamagers) {
        StringBuilder leaderboard = new StringBuilder();
        leaderboard.append("§e----------[BXH]---------\n");

        int topPlayers = Math.min(sortedDamagers.size(), 3);
        for (int i = 0; i < topPlayers; i++) {
            DamageMetaData data = sortedDamagers.get(i);
            Player player = data.getPlayer();
            if (player != null) {
                leaderboard.append(String.format("    §f%d. §a%s §f- §c%.2f\n",
                        i + 1,
                        player.getName(),
                        data.getTotalDamage()));
            }
        }

        leaderboard.append("§e------------------------");
        String finalMessage = leaderboard.toString();
        plugin.getServer().getOnlinePlayers().forEach(player ->
                player.sendMessage(finalMessage));
    }

    private void distributeRewards(Player player, DamageMetaData damageMetaData, ActiveMob mythicMob, List<String> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        player.sendMessage(String.format("You did %.2f damage to %s",
                damageMetaData.getTotalDamage(),
                mythicMob.getDisplayName()));

        rewards.forEach(reward -> executeCommand(reward.replace("{ten}", player.getName())));
    }

    public static void executeCommand(String command) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        Bukkit.dispatchCommand(console, command);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) ||
                !(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (!MythicBukkit.inst().getAPIHelper().isMythicMob(entity)) {
            return;
        }

        ActiveMob mythicMob = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(entity);
        String mobTypeId = mythicMob.getMobType();

        if (!validateMobConfiguration(mobTypeId, player)) {
            return;
        }

        updateDamageData(player, event.getDamage(), mobTypeId, mythicMob);
    }

    private boolean validateMobConfiguration(String mobTypeId, Player player) {
        if (plugin.data.getModData(mobTypeId) == null) {
            player.sendMessage("This mob is not in the config file: " + mobTypeId);
            return false;
        }
        return true;
    }

    private void updateDamageData(Player player, double damage, String mobTypeId, ActiveMob mythicMob) {
        damageData.computeIfAbsent(mobTypeId, k -> new ConcurrentHashMap<>());

        Map<UUID, DamageMetaData> mobDamageData = damageData.get(mobTypeId);
        DamageMetaData playerDamageData = mobDamageData.computeIfAbsent(
                player.getUniqueId(),
                k -> new DamageMetaData(player, plugin.data.getModData(mobTypeId))
        );

        playerDamageData.incrementDamage(damage);

        if (logger.isLoggable(java.util.logging.Level.FINE)) {
            logger.fine(String.format("Player %s dealt %.2f damage to %s (Total: %.2f)",
                    player.getName(), damage, mythicMob.getDisplayName(),
                    playerDamageData.getTotalDamage()));
        }
    }

    private static List<DamageMetaData> sortByTotalDamage(Map<UUID, DamageMetaData> map) {
        return map.values()
                .stream()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    public void cleanup() {
        damageData.values().forEach(mobData ->
                mobData.values().forEach(DamageMetaData::cleanup));
        damageData.clear();

        logger.info("EventListener resources cleaned up successfully");
    }
}