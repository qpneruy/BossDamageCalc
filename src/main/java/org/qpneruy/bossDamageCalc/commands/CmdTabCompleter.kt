package org.qpneruy.bossDamageCalc.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.qpneruy.bossDamageCalc.BossDamageCalc
import java.util.*

class CmdTabCompleter(plugin: BossDamageCalc) : TabCompleter {
    init {
        Objects.requireNonNull(plugin.getCommand("bossDamageCalc"))?.tabCompleter = this
    }

    override fun onTabComplete(
        commandSender: CommandSender,
        command: Command,
        s: String,
        strings: Array<String>
    ): List<String> {
        if (strings.size == 1) {
            return listOf("reload")
        }
        return listOf()
    }
}
