package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

/**
 * Menu graphique pour /wanted : affiche les joueurs recherchés sous forme de têtes.
 * Cliquer sur une tête désigne ce joueur comme cible du Tracker.
 */
public class WantedGUI implements Listener {

    private static class WantedHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    private final BountySMP plugin;

    public WantedGUI(BountySMP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new WantedHolder(), 27, Component.text("Recherchés (WANTED)", NamedTextColor.DARK_RED));

        List<PlayerData> candidates = plugin.getDataManager().topBounties(50);
        long windowSeconds = plugin.getConfig().getLong("danger.window-seconds", 600);
        long now = System.currentTimeMillis();

        int slot = 10;
        int shown = 0;
        for (PlayerData data : candidates) {
            if (shown >= 7) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(data.getUuid());
            // Ignore les entrées "fantômes" (joueur inexistant ou jamais connecté au serveur)
            if (op.getName() == null || !op.hasPlayedBefore()) {
                continue;
            }
            String name = op.getName();

            long recentKills = data.getRecentKillTimestamps().stream()
                    .filter(t -> (now - t) <= windowSeconds * 1000L)
                    .count();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(op);
            meta.displayName(Component.text("§c" + name).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("§7☠ Prime : §e" + (long) data.getBounty() + " coins").decoration(TextDecoration.ITALIC, false),
                    Component.text("§7⚔ Kills récents : §c" + recentKills).decoration(TextDecoration.ITALIC, false),
                    Component.text("").decoration(TextDecoration.ITALIC, false),
                    Component.text("§eClique pour traquer avec ton Tracker").decoration(TextDecoration.ITALIC, false)
            ));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            slot++;
            shown++;
        }

        if (shown == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(Component.text("§7Aucun joueur recherché pour le moment").decoration(TextDecoration.ITALIC, false));
            empty.setItemMeta(meta);
            inv.setItem(13, empty);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WantedHolder)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof WantedHolder)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD || !(clicked.getItemMeta() instanceof SkullMeta meta)) {
            return;
        }
        if (meta.getOwningPlayer() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!plugin.getShopGUI().hasTracker(player)) {
            player.sendMessage(Component.text("Tu as besoin d'un Tracker pour cibler un joueur (achète-le en boutique).", NamedTextColor.RED));
            return;
        }

        UUID targetUuid = meta.getOwningPlayer().getUniqueId();
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        data.setTrackerTarget(targetUuid);

        String targetName = meta.getOwningPlayer().getName() != null ? meta.getOwningPlayer().getName() : "ce joueur";
        player.sendMessage(Component.text("🎯 Tu traques désormais ", NamedTextColor.GOLD)
                .append(Component.text(targetName, NamedTextColor.RED))
                .append(Component.text(" avec ton Tracker.", NamedTextColor.GOLD)));
        player.closeInventory();
    }
}
