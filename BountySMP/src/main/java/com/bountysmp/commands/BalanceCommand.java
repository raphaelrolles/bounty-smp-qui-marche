package com.bountysmp.commands;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import com.bountysmp.util.Sfx;
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

/**
 * /balance : affiche ton solde de Bounty Coins (et la prime sur ta tête s'il y en a une).
 * /balance <joueur> : réservé aux admins (permission bountysmp.admin).
 */
public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final BountySMP plugin;

    public BalanceCommand(BountySMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            if (!sender.hasPermission("bountysmp.admin")) {
                sender.sendMessage(Component.text("Tu ne peux voir que ton propre solde : /balance", NamedTextColor.RED));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.isOnline() && !target.hasPlayedBefore()) {
                sender.sendMessage(Component.text("Joueur introuvable : " + args[0], NamedTextColor.RED));
                return true;
            }
            PlayerData data = plugin.getDataManager().get(target.getUniqueId());
            sender.sendMessage(Component.text("💰 " + args[0] + " : ", NamedTextColor.GOLD)
                    .append(Component.text((long) data.getCoins() + " coins", NamedTextColor.YELLOW))
                    .append(Component.text("  •  prime : " + (long) data.getBounty(), NamedTextColor.GRAY)));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Précise un joueur : /balance <joueur>", NamedTextColor.RED));
            return true;
        }
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        Component message = Component.text("💰 Solde : ", NamedTextColor.GOLD)
                .append(Component.text((long) data.getCoins() + " coins", NamedTextColor.YELLOW));
        if (data.getBounty() > 0) {
            message = message.append(Component.text("  •  🎯 Prime sur ta tête : ", NamedTextColor.GRAY))
                    .append(Component.text((long) data.getBounty() + " coins", NamedTextColor.RED));
        }
        player.sendActionBar(message);
        Sfx.click(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("bountysmp.admin")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
