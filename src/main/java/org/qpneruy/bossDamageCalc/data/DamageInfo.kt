package org.qpneruy.bossDamageCalc.data

import lombok.Getter
import org.bukkit.entity.Player
import java.util.*
import kotlin.concurrent.Volatile

@Getter
class DamageInfo(player: Player, entityData: ModData?) : Comparable<DamageInfo> {
    private val playerId: UUID
    public var player: Player
    private var entityData: ModData?

    @Volatile
    var totalDamage: Double

    init {
        this.player = player
        this.playerId = player.uniqueId
        this.entityData = entityData
        this.totalDamage = 0.0
    }

    @Synchronized
    fun incrementDamage(damage: Double) {
        if (damage > 0) this.totalDamage += damage
    }

    fun cleanup() {
        this.entityData = null
        this.totalDamage = 0.0
    }

    override fun compareTo(other: DamageInfo): Int {
        return java.lang.Double.compare(this.totalDamage, other.totalDamage)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is DamageInfo) return false
        return playerId == o.playerId
    }

    override fun hashCode(): Int {
        return playerId.hashCode()
    }
}