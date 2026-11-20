package com.bountysmp.commands;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
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

/** /stats [joueur] : statistiques de chasseur de primes. Le solde n'est visible que par le joueur lui-même ou un admin. */
public class StatsCommand implements CommandExecutor, TabCompleter {

    private final BountySMP plugin;

    public StatsCommand(BountySMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (!Players.isReal(target)) {
                sender.sendMessage(Component.text("Joueur introuvable : " + args[0], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(Component.text("Précise un joueur : /stats <joueur>", NamedTextColor.RED));
            return true;
        }

        PlayerData d = plugin.getDataManager().get(target.getUniqueId());
        boolean own = sender instanceof Player p && p.getUniqueId().equals(target.getUniqueId());
        boolean showPrivate = own || sender.hasPermission("bountysmp.admin");
        String name = target.getName() != null ? target.getName() : "?";
        String kd = d.getDeaths() == 0 ? String.valueOf(d.getKills()) : String.format("%.2f", d.getKills() / (double) d.getDeaths());
        long highest = (long) Math.max(d.getHighestBounty(), d.getBounty());

        sender.sendMessage(Component.text("📊 Stats de " + name, NamedTextColor.GOLD));
        sender.sendMessage(Component.text("⚔ Kills : " + d.getKills() + "  •  ☠ Morts : " + d.getDeaths() + "  •  K/D : " + kd, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("🎯 Primes encaissées : " + d.getBountiesClaimed() + "  •  🔝 Plus grosse prime : " + highest, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("🏆 Têtes vendues : " + d.getTrophiesSold() + "  •  🗝 Vols réussis : " + d.getPickpockets()
                + "  •  📜 Quêtes terminées : " + d.getQuestsDone(), NamedTextColor.YELLOW));
        if (showPrivate) {
            sender.sendMessage(Component.text("💰 Solde : " + (long) d.getCoins() + " coins  •  Prime actuelle : " + (long) d.getBounty(), NamedTextColor.GOLD));
        }
        Player online = target.getPlayer();
        if (online != null) {
            sender.sendMessage(Component.text("🏅 Succès : " + plugin.getAchievementUtil().countDone(online) + "/" + plugin.getAchievementUtil().total(), NamedTextColor.AQUA));
        }
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
