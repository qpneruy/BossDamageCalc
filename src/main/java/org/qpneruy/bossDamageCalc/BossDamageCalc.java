package org.qpneruy.bossDamageCalc;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.qpneruy.bossDamageCalc.commands.BossDamageCalcCmd;
import org.qpneruy.bossDamageCalc.commands.CmdTabCompleter;
import org.qpneruy.bossDamageCalc.data.ConfigReader;
import org.qpneruy.bossDamageCalc.events.EventListener;

import java.util.logging.Logger;
public final class BossDamageCalc extends JavaPlugin {
    @Getter
    private static BossDamageCalc instance;
    private EventListener eventListener;
    public ConfigReader data;
    @Override
    public void onEnable() {
        instance = this;
        StartupLog();
        if (!this.getDataFolder().exists()){
            if(!this.getDataFolder().mkdir())
                getLogger().severe("Failed to create plugin directory.");
            saveResource("config.toml", false);
        }

        Hooks();
        new BossDamageCalcCmd(this);
        new CmdTabCompleter(this);

        this.getServer().getPluginManager().registerEvents(eventListener, this);
    }

    @Override
    public void onDisable() {
        eventListener.cleanup();
        data.cleanup();
        instance = null;
    }

    private void Hooks() {
        if (this.getServer().getPluginManager().isPluginEnabled("MythicMobs")){
            getLogger().info("Successful Hook into MythicMobs!");
            this.data = new ConfigReader(this);
            this.eventListener = new EventListener(this);
        } else {
            getLogger().info("MythicMobs not found! Disabling plugin");
        }

    }

    private void StartupLog() {
        Logger sender = Bukkit.getLogger();
        sender.info("BossDamageCalc has been enabled!");
        sender.info("Version: 2024.12.30 - Author: qpneruy");
    }
}
