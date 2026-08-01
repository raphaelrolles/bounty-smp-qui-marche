package com.bountysmp.commands;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class WantedCommand implements CommandExecutor {

    private final BountySMP plugin;

    public WantedCommand(BountySMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<PlayerData> top = plugin.getDataManager().topBounties(10);

        sender.sendMessage(Component.text("===== WANTED =====", NamedTextColor.DARK_RED));
        if (top.isEmpty()) {
            sender.sendMessage(Component.text("Aucun joueur recherché pour le moment.", NamedTextColor.GRAY));
            return true;
        }

        long windowSeconds = plugin.getConfig().getLong("danger.window-seconds", 600);
        long now = System.currentTimeMillis();

        int rank = 1;
        for (PlayerData data : top) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(data.getUuid());
            String name = op.getName() != null ? op.getName() : "?";

            long recentKills = data.getRecentKillTimestamps().stream()
                    .filter(t -> (now - t) <= windowSeconds * 1000L)
                    .count();

            sender.sendMessage(Component.text(rank + ". ", NamedTextColor.GRAY)
                    .append(Component.text(name, NamedTextColor.GOLD)));
            sender.sendMessage(Component.text("   ☠ Prime : ", NamedTextColor.DARK_RED)
                    .append(Component.text((long) data.getBounty() + " coins", NamedTextColor.YELLOW)));
            sender.sendMessage(Component.text("   ⚔ Kills récents : ", NamedTextColor.GRAY)
                    .append(Component.text(recentKills, NamedTextColor.RED)));
            rank++;
        }
        return true;
    }
}
