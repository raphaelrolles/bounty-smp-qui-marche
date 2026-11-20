package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import com.bountysmp.quests.Quest;
import com.bountysmp.quests.QuestManager;
import com.bountysmp.util.Daily;
import com.bountysmp.util.GuiTitle;
import com.bountysmp.util.Icons;
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

import java.util.ArrayList;
import java.util.List;

/** Menu des 3 quêtes du jour, avec barres de progression. Accessible depuis /bounty. */
public class QuestGUI implements Listener {

    private static final int[] QUEST_SLOTS = {11, 13, 15};

    private static class QuestHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final BountySMP plugin;
    private final NamespacedKey actionKey;

    public QuestGUI(BountySMP plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "bounty_quest_action");
    }

    public void open(Player player) {
        QuestManager qm = plugin.getQuestManager();
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        qm.ensureCurrent(data);

        Inventory inv = Bukkit.createInventory(new QuestHolder(), 27,
                GuiTitle.of(plugin, GuiTitle.QUESTS, "Quêtes du jour", NamedTextColor.DARK_GREEN));

        List<Quest> quests = qm.today();
        int doneCount = 0;
        for (int i = 0; i < quests.size() && i < QUEST_SLOTS.length; i++) {
            Quest q = quests.get(i);
            boolean done = qm.isDone(data, q);
            if (done) {
                doneCount++;
            }
            inv.setItem(QUEST_SLOTS[i], questItem(q, qm.progress(data, q), done));
        }

        // résumé / bonus en haut
        int bonus = plugin.getConfig().getInt("quests.bonus-reward", 50);
        List<String> lore = new ArrayList<>();
        lore.add("§7Quêtes terminées : §f" + doneCount + "/" + quests.size());
        lore.add("§7Bonus si tout est fini : §6+" + bonus + " coins");
        lore.add("");
        lore.add("§7Nouvelles quêtes dans §f" + Daily.untilReset(plugin));
        if (data.isQuestBonusClaimed()) {
            inv.setItem(4, Icons.gui(plugin, Icons.CHECK, Material.LIME_DYE, "§a★ Bonus du jour reçu !", lore));
        } else {
            inv.setItem(4, Icons.gui(plugin, Icons.CLOCK, Material.CLOCK, "§eQuêtes du jour", lore));
        }

        ItemStack back = Icons.gui(plugin, Icons.BACK, Material.BARRIER, "§7« Retour au menu", null);
        ItemMeta bm = back.getItemMeta();
        bm.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
        back.setItemMeta(bm);
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    private ItemStack questItem(Quest q, int progress, boolean done) {
        List<String> lore = new ArrayList<>();
        lore.add(q.tier().label());
        lore.add("");
        lore.add("§7Progression : §f" + progress + "§7/§f" + q.target());
        lore.add(progressBar(progress, q.target()));
        lore.add("");
        lore.add("§7Récompense : §6" + q.reward() + " coins");
        if (done) {
            lore.add("");
            lore.add("§a✔ Terminée !");
            return Icons.gui(plugin, Icons.CHECK, Material.LIME_DYE, "§a" + q.title(), lore);
        }
        ItemStack item = new ItemStack(q.type().icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e" + q.title()).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(l -> Component.text(l).decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private String progressBar(int progress, int target) {
        int filled = (int) Math.round(10.0 * Math.min(progress, target) / Math.max(1, target));
        return "§a" + "█".repeat(filled) + "§8" + "█".repeat(10 - filled);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuestHolder)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof QuestHolder)) {
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
