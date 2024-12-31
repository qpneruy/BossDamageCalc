package org.qpneruy.bossDamageCalc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.qpneruy.bossDamageCalc.BossDamageCalc;

import java.util.List;
import java.util.Objects;

public class CmdTabCompleter implements TabCompleter {
    public CmdTabCompleter(BossDamageCalc plugin) {
        Objects.requireNonNull(plugin.getCommand("bossDamageCalc")).setTabCompleter(this);
    }
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
