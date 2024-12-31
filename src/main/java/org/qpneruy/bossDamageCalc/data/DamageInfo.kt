package org.qpneruy.bossDamageCalc.data;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class DamageInfo implements Comparable<DamageInfo> {
    private final UUID playerId;
    private Player player;
    private ModData entityData;
    private volatile double totalDamage;

    public DamageInfo(Player player, ModData entityData) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.entityData = entityData;
        this.totalDamage = 0.0;
    }

    public synchronized void incrementDamage(double damage) {
        if (damage > 0) this.totalDamage += damage;

    }

    public void cleanup() {
        this.player = null;
        this.entityData = null;
        this.totalDamage = 0.0;
    }

    @Override
    public int compareTo(DamageInfo other) {
        return Double.compare(this.totalDamage, other.totalDamage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DamageInfo that)) return false;
        return playerId.equals(that.playerId);
    }

    @Override
    public int hashCode() {
        return playerId.hashCode();
    }
}