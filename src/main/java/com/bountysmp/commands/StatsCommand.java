package com.bountysmp.commands;

import com.bountysmp.BountySMP;
import com.bountysmp.util.Players;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/** /stats [joueur] : ouvre le menu des statistiques. */
public class StatsCommand implements CommandExecutor, TabCompleter {

    private final BountySMP plugin;

    public StatsCommand(BountySMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Component.text("Cette commande doit être utilisée en jeu.", NamedTextColor.RED));
            return true;
        }
        OfflinePlayer target = viewer;
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (!Players.isReal(target)) {
                viewer.sendMessage(Component.text("Joueur introuvable : " + args[0], NamedTextColor.RED));
                return true;
            }
        }
        plugin.getStatsGUI().open(viewer, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
