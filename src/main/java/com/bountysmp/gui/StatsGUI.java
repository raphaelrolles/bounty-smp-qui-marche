package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import com.bountysmp.util.GuiTitle;
import com.bountysmp.util.Icons;
import com.bountysmp.util.MenuTheme;
import com.bountysmp.util.Sfx;
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

import java.util.ArrayList;
import java.util.List;

/** Menu des statistiques d'un joueur (/stats ou clic sur ton profil dans /bounty). */
public class StatsGUI implements Listener {

    private static class StatsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final BountySMP plugin;
    private final NamespacedKey actionKey;

    public StatsGUI(BountySMP plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "bounty_stats_action");
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(l -> Component.text(l).decoration(TextDecoration.ITALIC, false)).toList());
        it.setItemMeta(meta);
        return it;
    }

    public void open(Player viewer, OfflinePlayer target) {
        PlayerData viewerData = plugin.getDataManager().get(viewer.getUniqueId());
        PlayerData d = plugin.getDataManager().get(target.getUniqueId());
        boolean own = viewer.getUniqueId().equals(target.getUniqueId());
        boolean priv = own || viewer.hasPermission("bountysmp.admin");
        String name = target.getName() != null ? target.getName() : "?";

        Inventory inv = Bukkit.createInventory(new StatsHolder(), 27,
                GuiTitle.of(plugin, MenuTheme.from(viewerData.getMenuTheme()).glyph(), "Statistiques", NamedTextColor.DARK_RED));

        String kd = d.getDeaths() == 0 ? String.valueOf(d.getKills()) : String.format("%.2f", d.getKills() / (double) d.getDeaths());
        long highest = (long) Math.max(d.getHighestBounty(), d.getBounty());
        Player online = target.getPlayer();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = head.getItemMeta();
        if (headMeta instanceof SkullMeta skull) {
            skull.setOwningPlayer(target);
        }
        headMeta.displayName(Component.text("§6Statistiques de " + name).decoration(TextDecoration.ITALIC, false));
        head.setItemMeta(headMeta);
        inv.setItem(4, head);

        inv.setItem(10, item(Material.IRON_SWORD, "§eCombats",
                List.of("§7Kills : §f" + d.getKills(), "§7Morts : §f" + d.getDeaths(), "§7K/D : §f" + kd)));
        inv.setItem(12, item(Material.SKELETON_SKULL, "§cPrimes",
                List.of("§7Primes encaissées : §f" + d.getBountiesClaimed(), "§7Plus grosse prime : §f" + highest + " coins")));
        inv.setItem(14, item(Material.PLAYER_HEAD, "§6Trophées et vols",
                List.of("§7Têtes vendues : §f" + d.getTrophiesSold(), "§7Vols réussis : §f" + d.getPickpockets())));
        inv.setItem(16, item(Material.WRITABLE_BOOK, "§aQuêtes",
                List.of("§7Quêtes terminées : §f" + d.getQuestsDone())));

        if (priv) {
            ItemStack coins = new ItemStack(Material.GOLD_INGOT);
            ItemMeta cm = coins.getItemMeta();
            cm.displayName(Component.text("§6Solde").decoration(TextDecoration.ITALIC, false));
            cm.lore(List.of(
                    Component.text("§7Coins : §e" + (long) d.getCoins()).decoration(TextDecoration.ITALIC, false),
                    Component.text("§7Prime actuelle : §c" + (long) d.getBounty()).decoration(TextDecoration.ITALIC, false)));
            Icons.cmd(plugin, cm, Icons.COIN);
            coins.setItemMeta(cm);
            inv.setItem(20, coins);
        } else {
            inv.setItem(20, item(Material.BARRIER, "§8Solde privé", List.of("§7Seul le joueur (ou un admin) peut le voir.")));
        }
        String achievements = online != null
                ? plugin.getAchievementUtil().countDone(online) + "/" + plugin.getAchievementUtil().total()
                : "joueur hors ligne";
        inv.setItem(24, item(Material.NETHER_STAR, "§bSuccès", List.of("§7Débloqués : §f" + achievements)));

        ItemStack back = Icons.gui(plugin, Icons.BACK, Material.BARRIER, "§7« Retour au menu", null);
        ItemMeta bm = back.getItemMeta();
        bm.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
        back.setItemMeta(bm);
        inv.setItem(22, back);

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StatsHolder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof StatsHolder)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        String id = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (!"back".equals(id)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Sfx.click(player);
        plugin.getBountyMenuGUI().open(player);
    }
}
