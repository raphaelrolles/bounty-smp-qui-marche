package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Menu graphique pour /bounty (sans argument) : profil du joueur + raccourcis
 * vers la boutique et la liste des recherchés.
 */
public class BountyMenuGUI implements Listener {

    private static class BountyMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    private final BountySMP plugin;
    private final NamespacedKey actionKey;

    public BountyMenuGUI(BountySMP plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "bounty_menu_action");
    }

    public void open(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(new BountyMenuHolder(), 27, Component.text("Bounty SMP", NamedTextColor.DARK_RED));

        ItemStack profile = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta profileMeta = profile.getItemMeta();
        if (profileMeta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
        }
        profileMeta.displayName(Component.text("§6Ton profil").decoration(TextDecoration.ITALIC, false));
        profileMeta.lore(List.of(
                Component.text("§7Coins : §e" + (long) data.getCoins()).decoration(TextDecoration.ITALIC, false),
                Component.text("§7Prime sur toi : §c" + (long) data.getBounty()).decoration(TextDecoration.ITALIC, false),
                Component.text("§7Primes récupérées : §a" + data.getBountiesClaimed()).decoration(TextDecoration.ITALIC, false)
        ));
        profile.setItemMeta(profileMeta);
        inv.setItem(13, profile);

        inv.setItem(4, buildActionItem(Material.NETHER_STAR, "§6Top Chasseurs", "open_top",
                List.of("§7Classement des meilleurs.")));

        inv.setItem(11, buildActionItem(Material.CHEST, "§aOuvrir la Boutique", "open_shop",
                List.of("§7Dépense tes coins.")));

        inv.setItem(15, buildActionItem(Material.SKELETON_SKULL, "§cVoir les Recherchés", "open_wanted",
                List.of("§7Liste des joueurs traqués.")));

        player.openInventory(inv);
    }

    private ItemStack buildActionItem(Material material, String name, String id, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(l -> Component.text(l).decoration(TextDecoration.ITALIC, false)).toList());
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BountyMenuHolder)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof BountyMenuHolder)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        String id = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (id == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        switch (id) {
            case "open_shop" -> plugin.getShopGUI().open(player);
            case "open_top" -> plugin.getTopGUI().open(player);
            case "open_wanted" -> plugin.getWantedGUI().open(player);
        }
    }
}
