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

/** Menu de personnalisation : chaque joueur choisit l'apparence de SON menu /bounty. */
public class ThemeGUI implements Listener {

    private static final int[] THEME_SLOTS = {10, 12, 14, 16, 19, 21, 23, 25};

    private static class ThemeHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final BountySMP plugin;
    private final NamespacedKey actionKey;

    public ThemeGUI(BountySMP plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "bounty_theme_action");
    }

    public void open(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        MenuTheme current = MenuTheme.from(data.getMenuTheme());

        Inventory inv = Bukkit.createInventory(new ThemeHolder(), 27,
                GuiTitle.of(plugin, GuiTitle.THEME_MENU, "Personnaliser le menu", NamedTextColor.DARK_PURPLE));

        inv.setItem(4, Icons.gui(plugin, Icons.PALETTE, Material.PAINTING, "§dPersonnaliser ton menu",
                List.of("§7Choisis l'apparence de ton menu /bounty.", "", "§7Thème actuel : §f" + current.label())));

        MenuTheme[] themes = MenuTheme.values();
        for (int i = 0; i < themes.length && i < THEME_SLOTS.length; i++) {
            MenuTheme t = themes[i];
            boolean selected = t == current;
            ItemStack item = new ItemStack(t.icon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text((selected ? "§a✔ " : "§f") + t.label()).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text(selected ? "§aThème actuel" : "§7Clique pour appliquer").decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, t.id());
            item.setItemMeta(meta);
            inv.setItem(THEME_SLOTS[i], item);
        }

        ItemStack back = Icons.gui(plugin, Icons.BACK, Material.BARRIER, "§7« Retour au menu", null);
        ItemMeta bm = back.getItemMeta();
        bm.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
        back.setItemMeta(bm);
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ThemeHolder)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof ThemeHolder)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        String id = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (id == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Sfx.click(player);
        if (id.equals("back")) {
            plugin.getBountyMenuGUI().open(player);
            return;
        }
        MenuTheme chosen = MenuTheme.from(id);
        plugin.getDataManager().get(player.getUniqueId()).setMenuTheme(chosen.id());
        Sfx.purseWin(plugin, player);
        // on rouvre directement le menu principal pour voir le résultat
        plugin.getBountyMenuGUI().open(player);
        player.sendActionBar(Component.text("🎨 Thème « " + chosen.label() + " » appliqué à ton menu.", NamedTextColor.LIGHT_PURPLE));
    }
}
