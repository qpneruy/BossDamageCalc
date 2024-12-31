package org.qpneruy.bossDamageCalc.events;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
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
import org.qpneruy.bossDamageCalc.data.ConfigReader;
import org.qpneruy.bossDamageCalc.data.DamageInfo;
import org.qpneruy.bossDamageCalc.data.ModData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EventListener implements Listener {
    private final Logger logger;
    private static final int MAX_REWARD_RANK = 10;

    private final Map<String, Map<UUID, DamageInfo>> damageData;
    private final BossDamageCalc plugin;
    private final ConsoleCommandSender console;
    private final BukkitAPIHelper apiHelper;
    private final ConfigReader data;

    public EventListener(BossDamageCalc plugin) {
        this.plugin = plugin;
        this.damageData = new ConcurrentHashMap<>();
        this.logger = plugin.getLogger();
        this.console = Bukkit.getConsoleSender();
        this.apiHelper = MythicBukkit.inst().getAPIHelper();
        this.data = plugin.data;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!apiHelper.isMythicMob(event.getEntity())) {
            return;
        }
        System.out.println("Checkpoint 5");
        ActiveMob mythicMob = apiHelper.getMythicMobInstance(event.getEntity());
        String mobTypeId = mythicMob.getMobType();
        Map<UUID, DamageInfo> mobDamageData = damageData.get(mobTypeId);

        if (mobDamageData == null || mobDamageData.isEmpty()) return;

        processRewards(mythicMob, mobDamageData);
        damageData.remove(mobTypeId);
    }

    private void processRewards(ActiveMob mythicMob, Map<UUID, DamageInfo> mobDamageData) {
        ModData modData = data.getModData(mythicMob.getMobType());
        if (modData == null) {
            logger.warning("No ModData found for mob type: " + mythicMob.getMobType());
            return;
        }
        List<DamageInfo> sortedDamagers = sortByTotalDamage(mobDamageData);
        Map<Integer, List<String>> rewards = modData.getRewards();
        displayLeaderboard(sortedDamagers);

        for (int rank = 0; rank < Math.min(sortedDamagers.size(), MAX_REWARD_RANK); rank++) {
            DamageInfo damageInfo = sortedDamagers.get(rank);
            distributeRewards(damageInfo.getPlayer(), damageInfo, mythicMob, rewards.get(rank + 1));
        }
    }

    private void displayLeaderboard(List<DamageInfo> sortedDamagers) {
        StringBuilder leaderboard = new StringBuilder();
        leaderboard.append("§e----------[BXH]---------\n");

        int topPlayers = Math.min(sortedDamagers.size(), 3);
        for (int i = 0; i < topPlayers; i++) {
            DamageInfo data = sortedDamagers.get(i);
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

    private void distributeRewards(Player player, DamageInfo damageInfo, ActiveMob mythicMob, List<String> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }
        player.sendMessage(String.format("You did %.2f damage to %s",
                damageInfo.getTotalDamage(),
                mythicMob.getDisplayName()));

        rewards.forEach(reward -> Bukkit.dispatchCommand(console, reward.replace("{ten}", player.getName())));
    }


    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) ||
                !(event.getEntity() instanceof LivingEntity entity)) return;

        if (!apiHelper.isMythicMob(entity)) return;


        ActiveMob mythicMob = apiHelper.getMythicMobInstance(entity);
        String mobTypeId = mythicMob.getMobType();

        if (data.getModData(mobTypeId) == null) return;
        updateDamageData(player, event.getDamage(), mobTypeId, mythicMob);
    }

    private void updateDamageData(Player player, double damage, String mobTypeId, ActiveMob mythicMob) {
        damageData.computeIfAbsent(mobTypeId, k -> new ConcurrentHashMap<>());

        Map<UUID, DamageInfo> mobDamageData = damageData.get(mobTypeId);
        DamageInfo playerDamageData = mobDamageData.computeIfAbsent(
                player.getUniqueId(),
                k -> new DamageInfo(player, data.getModData(mobTypeId))
        );

        playerDamageData.incrementDamage(damage);
        if (logger.isLoggable(java.util.logging.Level.FINE)) {
            logger.fine(String.format("Player %s dealt %.2f damage to %s (Total: %.2f)",
                    player.getName(), damage, mythicMob.getDisplayName(),
                    playerDamageData.getTotalDamage()));
        }
    }

    private static List<DamageInfo> sortByTotalDamage(Map<UUID, DamageInfo> map) {
        return map.values()
                .stream()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    public void cleanup() {
        damageData.values().forEach(mobData ->
                mobData.values().forEach(DamageInfo::cleanup));
        damageData.clear();

        logger.info("EventListener resources cleaned up successfully");
    }
}