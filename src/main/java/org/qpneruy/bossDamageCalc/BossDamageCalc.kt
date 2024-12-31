package org.qpneruy.bossDamageCalc

import lombok.Getter
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.qpneruy.bossDamageCalc.commands.BossDamageCalcCmd
import org.qpneruy.bossDamageCalc.commands.CmdTabCompleter
import org.qpneruy.bossDamageCalc.data.ConfigReader
import org.qpneruy.bossDamageCalc.events.EventListener

class BossDamageCalc : JavaPlugin() {
    private var eventListener: EventListener? = null
    var data: ConfigReader? = null
    override fun onEnable() {
        instance = this
        StartupLog()
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdir()) logger.severe("Failed to create plugin directory.")
            saveResource("config.toml", false)
        }

        Hooks()
        BossDamageCalcCmd(this)
        CmdTabCompleter(this)

        server.pluginManager.registerEvents(eventListener!!, this)
    }

    override fun onDisable() {
        if (!(eventListener == null || data == null)) {
            eventListener!!.cleanup()
            data!!.cleanup()
        }
        instance = null
    }

    private fun Hooks() {
        if (server.pluginManager.isPluginEnabled("MythicMobs")) {
            logger.info("Successful Hook into MythicMobs!")
            this.data = ConfigReader(this)
            this.eventListener = EventListener(this)
        } else {
            logger.info("MythicMobs not found! Disabling plugin")
        }
    }

    private fun StartupLog() {
        val sender = Bukkit.getLogger()
        sender.info("BossDamageCalc has been enabled!")
        sender.info("Version: 2024.12.30 - Author: qpneruy")
    }

    companion object {
        @Getter
        private var instance: BossDamageCalc? = null
    }
}
