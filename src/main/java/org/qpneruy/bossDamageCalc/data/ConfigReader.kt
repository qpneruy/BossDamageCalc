package org.qpneruy.bossDamageCalc.data

import com.moandjiezana.toml.Toml
import org.qpneruy.bossDamageCalc.BossDamageCalc
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.logging.Logger

class ConfigReader(plugin: BossDamageCalc) {
    private val modDataMap: ConcurrentHashMap<String, ModData> = ConcurrentHashMap(INITIAL_CAPACITY)
    private val configPath: Path = Path.of(plugin.dataFolder.path, "config.toml")
    private val logger: Logger = plugin.logger

    init {
        loadConfig()
    }

    fun reload() {
        loadConfig()
    }

    private fun loadConfig() {
        try {
            val tables = Toml().read(configPath.toFile()).toMap()
            processConfigTables(tables)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load config file: $configPath", e)
        }
    }

    private fun processConfigTables(tables: Map<String, Any>) {
        modDataMap.clear()

        for ((_, value) in tables) {
            if (value is Map<*, *>) {
                processTable(value as Map<String, Any>)
            }
        }
    }

    private fun processTable(table: Map<String, Any>) {
        val modIdObj = table["ModId"]
        if (modIdObj is String && table.containsKey("Rewards")) {
            val rewardsList = table["Rewards"] as List<Map<String, Any>>?
            val rewardsMap = processRewards(rewardsList!!, rewardsList.size)
            val modData = ModData(modIdObj, rewardsMap)
            modDataMap[modIdObj] = modData
        }
    }

    private fun processRewards(rewardsList: List<Map<String, Any>>, size: Int): HashMap<Int, List<String>?> {
        val rewardsMap = HashMap<Int, List<String>?>(size)

        for (reward in rewardsList) {
            val rank = (reward["Rank"] as Number).toInt()
            val rewards = reward["Rewards"] as List<String>?
            rewardsMap[rank] = rewards
        }

        return rewardsMap
    }

    fun getModData(modId: String): ModData? {
        return modDataMap[modId]
    }

    fun cleanup() {
        modDataMap.values.forEach(Consumer { obj: ModData -> obj.cleanup() })
        modDataMap.clear()
        logger.info("ConfigReader resources cleaned up successfully")
    }

    companion object {
        private const val INITIAL_CAPACITY = 16
    }
}