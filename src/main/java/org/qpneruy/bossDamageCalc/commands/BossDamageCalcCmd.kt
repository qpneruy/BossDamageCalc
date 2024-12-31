package org.qpneruy.bossDamageCalc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.qpneruy.bossDamageCalc.BossDamageCalc;

import java.util.Objects;

public class BossDamageCalcCmd implements CommandExecutor {

    public BossDamageCalcCmd(BossDamageCalc plugin) {
        Objects.requireNonNull(plugin.getCommand("bossDamageCalc")).setExecutor(this);
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            commandSender.sendMessage("§aBossDamageCalc v2024.12.30");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            BossDamageCalc.getInstance().data.reload();
            commandSender.sendMessage("§aBossDamageCalc config reloaded.");
            return true;
        }
        return true;
    }
}
