package com.bountysmp.commands;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WantedCommand implements CommandExecutor {

    private final BountySMP plugin;

    public WantedCommand(BountySMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Cette commande doit être utilisée en jeu.", NamedTextColor.RED));
            return true;
        }
        plugin.getWantedGUI().open(player);
        return true;
    }
}
