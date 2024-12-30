package org.qpneruy.bossDamageCalc.data;

import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
public class ModData {
    private String modId;
    private final Map<Integer, List<String>> rewards;

    public ModData(String modId, Map<Integer, List<String>> rewards) {
        this.modId = modId;
        this.rewards = rewards;
    }

    public void cleanup() {
        rewards.values().forEach(List::clear);
        rewards.clear();
        this.modId = null;
    }
}