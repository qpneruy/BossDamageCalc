package org.qpneruy.bossDamageCalc.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.qpneruy.bossDamageCalc.BossDamageCalc
import java.util.*

class BossDamageCalcCmd(plugin: BossDamageCalc) : CommandExecutor {
    private val plugin: BossDamageCalc

    init {
        Objects.requireNonNull(plugin.getCommand("bossDamageCalc"))?.setExecutor(this)
        this.plugin = plugin
    }

    override fun onCommand(commandSender: CommandSender, command: Command, s: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            commandSender.sendMessage("§aBossDamageCalc v2024.12.30")
            return true
        }

        if (args[0].equals("reload", ignoreCase = true)) {
            plugin.data?.reload()
            commandSender.sendMessage("§aBossDamageCalc config reloaded.")
            return true
        }
        return true
    }
}
