package org.qpneruy.bossDamageCalc.events

import io.lumine.mythic.bukkit.BukkitAPIHelper
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.mobs.ActiveMob
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.qpneruy.bossDamageCalc.BossDamageCalc
import org.qpneruy.bossDamageCalc.data.ConfigReader
import org.qpneruy.bossDamageCalc.data.DamageInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.logging.Level
import java.util.stream.Collectors
import kotlin.math.min

class EventListener(private val plugin: BossDamageCalc) : Listener {
    private val logger = plugin.logger

    private val damageData: MutableMap<String, MutableMap<UUID, DamageInfo>> =
        ConcurrentHashMap()
    private val console = Bukkit.getConsoleSender()
    private val apiHelper: BukkitAPIHelper = MythicBukkit.inst().apiHelper
    private val data: ConfigReader? = plugin.data

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (!apiHelper.isMythicMob(event.entity)) return

        val mythicMob = apiHelper.getMythicMobInstance(event.entity)
        val mobTypeId = mythicMob.mobType
        val mobDamageData: Map<UUID, DamageInfo>? = damageData[mobTypeId]

        if (mobDamageData == null || mobDamageData.isEmpty()) return

        processRewards(mythicMob, mobDamageData)
        damageData.remove(mobTypeId)
    }

    private fun processRewards(mythicMob: ActiveMob, mobDamageData: Map<UUID, DamageInfo>) {
        val modData = data!!.getModData(mythicMob.mobType)
        if (modData == null) {
            logger.warning("No ModData found for mob type: " + mythicMob.mobType)
            return
        }
        val sortedDamagers = sortByTotalDamage(mobDamageData)
        val rewards = modData.rewards
        displayLeaderboard(sortedDamagers)

        for (rank in 0..<min(sortedDamagers.size.toDouble(), MAX_REWARD_RANK.toDouble()).toInt()) {
            val damageInfo = sortedDamagers[rank]
            distributeRewards(damageInfo.player, damageInfo, mythicMob, rewards[rank + 1])
        }
    }

    private fun displayLeaderboard(sortedDamagers: List<DamageInfo>) {
        val leaderboard = StringBuilder()
        leaderboard.append("§e----------[BXH]---------\n")

        val topPlayers = min(sortedDamagers.size.toDouble(), 3.0).toInt()
        for (i in 0..<topPlayers) {
            val data = sortedDamagers[i]
            val player = data.player
            if (player != null) {
                leaderboard.append(
                    String.format(
                        "    §f%d. §a%s §f- §c%.2f\n",
                        i + 1,
                        player.name,
                        data.totalDamage
                    )
                )
            }
        }
        leaderboard.append("§e------------------------")
        val finalMessage = leaderboard.toString()
        plugin.server.onlinePlayers.forEach { player: Player -> player.sendMessage(finalMessage) }
    }

    private fun distributeRewards(
        player: Player,
        damageInfo: DamageInfo,
        mythicMob: ActiveMob,
        rewards: List<String>?
    ) {
        if (rewards.isNullOrEmpty()) return

        player.sendMessage(
            String.format(
                "You did %.2f damage to %s",
                damageInfo.totalDamage,
                mythicMob.displayName
            )
        )

        rewards.forEach(Consumer { reward: String ->
            Bukkit.dispatchCommand(
                console,
                reward.replace("{ten}", player.name)
            )
        })
    }


    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damager !is Player || event.entity !is LivingEntity) return

        if (!apiHelper.isMythicMob(event.entity)) return


        val mythicMob = apiHelper.getMythicMobInstance(event.entity)
        val mobTypeId = mythicMob.mobType

        if (data!!.getModData(mobTypeId) == null) return
        updateDamageData((event.damager as Player).getPlayer(), event.damage, mobTypeId, mythicMob)
    }

    private fun updateDamageData(player: Player?, damage: Double, mobTypeId: String, mythicMob: ActiveMob) {
        damageData.computeIfAbsent(mobTypeId) { k: String? -> ConcurrentHashMap() }

        val mobDamageData = damageData[mobTypeId]!!
        val playerDamageData = player?.let {
            mobDamageData.computeIfAbsent(
                it.uniqueId
            ) { k: UUID? -> DamageInfo(player, data!!.getModData(mobTypeId)) }
        }

        if (playerDamageData != null) {
            playerDamageData.incrementDamage(damage)
        }
        if (logger.isLoggable(Level.FINE)) {
            if (playerDamageData != null) {
                logger.fine(
                    String.format(
                        "Player %s dealt %.2f damage to %s (Total: %.2f)",
                        player.name, damage, mythicMob.displayName,
                        playerDamageData.totalDamage
                    )
                )
            }
        }
    }

    fun cleanup() {
        damageData.values.forEach(Consumer<Map<UUID, DamageInfo>> { mobData: Map<UUID, DamageInfo> ->
            mobData.values.forEach(
                Consumer { obj: DamageInfo -> obj.cleanup() })
        })
        damageData.clear()

        logger.info("EventListener resources cleaned up successfully")
    }

    companion object {
        private const val MAX_REWARD_RANK = 10

        private fun sortByTotalDamage(map: Map<UUID, DamageInfo>): List<DamageInfo> {
            return map.values
                .stream()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList())
        }
    }
}