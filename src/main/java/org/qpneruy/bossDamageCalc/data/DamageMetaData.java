package org.qpneruy.bossDamageCalc.data;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class DamageMetaData implements Comparable<DamageMetaData> {
    private final UUID playerId;
    private Player player;
    private ModData entityData;
    private volatile double totalDamage;

    public DamageMetaData(Player player, ModData entityData) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.entityData = entityData;
        this.totalDamage = 0.0;
    }

    public void cleanup() {

        this.player = null;
        entityData.cleanup();
        this.entityData = null;
        this.totalDamage = 0.0;
    }


    public synchronized void incrementDamage(double damage) {
        if (damage > 0) {
            this.totalDamage += damage;
        }
    }

    @Override
    public int compareTo(DamageMetaData other) {
        return Double.compare(this.totalDamage, other.totalDamage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DamageMetaData)) return false;
        DamageMetaData that = (DamageMetaData) o;
        return playerId.equals(that.playerId);
    }

    @Override
    public int hashCode() {
        return playerId.hashCode();
    }
}