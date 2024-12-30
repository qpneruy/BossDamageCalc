package org.qpneruy.bossDamageCalc.data;

import com.moandjiezana.toml.Toml;
import org.qpneruy.bossDamageCalc.BossDamageCalc;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ConfigReader implements AutoCloseable {
    private final ConcurrentHashMap<String, ModData> modDataMap;
    private static final int INITIAL_CAPACITY = 16;
    private final Path configPath;
    private final Logger logger;
    private boolean isActive;

    public ConfigReader(BossDamageCalc plugin) {
        this.modDataMap = new ConcurrentHashMap<>(INITIAL_CAPACITY);
        this.configPath = Path.of(plugin.getDataFolder().getPath(), "config.yml");
        this.logger = plugin.getLogger();
        this.isActive = true;
        loadConfig();
    }

    @Override
    public void close() {
        cleanup();
    }

    public void cleanup() {
        if (!isActive) {
            return;
        }

        modDataMap.values().forEach(ModData::cleanup);
        modDataMap.clear();
        isActive = false;
        logger.info("ConfigReader resources cleaned up successfully");
    }
    public void reload() {
        loadConfig();
    }
    private void loadConfig() {
        try {
            Map<String, Object> tables = new Toml().read(configPath.toFile()).toMap();
            processConfigTables(tables);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config file: " + configPath, e);
        }
    }

    private void processConfigTables(Map<String, Object> tables) {
        modDataMap.clear();

        for (Map.Entry<String, Object> entry : tables.entrySet()) {
            if (entry.getValue() instanceof Map) {
                processTable((Map<String, Object>) entry.getValue());
            }
        }
    }

    private void processTable(Map<String, Object> table) {
        Object modIdObj = table.get("ModId");
        if (modIdObj instanceof String modId && table.containsKey("Rewards")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rewardsList = (List<Map<String, Object>>) table.get("Rewards");
            HashMap<Integer, List<String>> rewardsMap = processRewards(rewardsList, rewardsList.size());
            ModData modData = new ModData(modId, rewardsMap);
            modDataMap.put(modId, modData);
        }
    }

    private HashMap<Integer, List<String>> processRewards(List<Map<String, Object>> rewardsList, int size) {
        HashMap<Integer, List<String>> rewardsMap = new HashMap<>(size);

        for (Map<String, Object> reward : rewardsList) {
            int rank = ((Number) reward.get("Rank")).intValue();
            @SuppressWarnings("unchecked")
            List<String> rewards = (List<String>) reward.get("Rewards");
            rewardsMap.put(rank, rewards);
        }

        return rewardsMap;
    }

    public ModData getModData(String modId) {
        return modDataMap.get(modId);
    }

    public void clearResources() {
        modDataMap.clear();
    }
}