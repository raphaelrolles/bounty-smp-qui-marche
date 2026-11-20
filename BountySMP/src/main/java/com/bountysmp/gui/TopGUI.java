package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.util.Players;
import com.bountysmp.util.GuiTitle;
import com.bountysmp.util.Icons;
import com.bountysmp.util.Sfx;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Menu graphique pour /bounty top : classement des meilleurs chasseurs (primes récupérées).
 */
public class TopGUI implements Listener {

    private static class TopHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    private final BountySMP plugin;
    private final NamespacedKey actionKey;

    public TopGUI(BountySMP plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "bounty_top_action");
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new TopHolder(), 27, GuiTitle.of(plugin, GuiTitle.TOP, "Top Chasseurs", NamedTextColor.DARK_RED));

        List<PlayerData> candidates = plugin.getDataManager().topHunters(50);
        int slot = 10;
        int shown = 0;
        int rank = 1;
        for (PlayerData data : candidates) {
            if (shown >= 7) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(data.getUuid());
            if (!Players.isReal(op)) {
                rank++;
                continue;
            }
            String name = op.getName();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(op);
            meta.displayName(Component.text("§6#" + rank + " §f" + name).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("§7Primes récupérées : §a" + data.getBountiesClaimed()).decoration(TextDecoration.ITALIC, false)
            ));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            slot++;
            shown++;
            rank++;
        }

        if (shown == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(Component.text("§7Personne n'a encore récupéré de prime").decoration(TextDecoration.ITALIC, false));
            empty.setItemMeta(meta);
            inv.setItem(13, empty);
        }

        ItemStack back = Icons.gui(plugin, Icons.BACK, Material.BARRIER, "§7« Retour au menu", null);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
        back.setItemMeta(backMeta);
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TopHolder)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof TopHolder)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        String id = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if ("back".equals(id)) {
            Sfx.click(player);
            plugin.getBountyMenuGUI().open(player);
        }
    }
}
