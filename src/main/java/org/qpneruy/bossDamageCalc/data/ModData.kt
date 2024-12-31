package org.qpneruy.bossDamageCalc.data

import lombok.Getter
import java.util.function.Consumer

@Getter
class ModData(private var modId: String?, val rewards: MutableMap<Int, List<String>?>) {
    fun cleanup() {
        rewards.values.forEach { obj: List<String>? -> (obj as? MutableList<String>)?.clear() }
        rewards.clear()
        this.modId = null
    }
}