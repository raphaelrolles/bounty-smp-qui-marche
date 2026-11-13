package com.bountysmp.util;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * CustomModelData du resource pack Bounty SMP (voir README du pack) et petits utilitaires.
 */
public final class Icons {

    // Items custom (objet de base vanilla indiqué à côté)
    public static final int TRACKER = 1001;   // ECHO_SHARD
    public static final int SMOKE = 1002;     // FIREWORK_ROCKET
    public static final int RAGE = 1003;      // GLASS_BOTTLE
    public static final int CHAOS = 1004;     // SNOWBALL
    public static final int DASH = 1005;      // FEATHER
    public static final int STINK = 1006;     // FERMENTED_SPIDER_EYE
    public static final int PURSE = 1007;     // SUSPICIOUS_STEW
    public static final int SPY = 1008;       // ENDER_EYE
    public static final int POUCH = 1009;     // STRING
    public static final int FAKE = 1010;      // GOLD_NUGGET
    public static final int RECALL = 1011;    // AMETHYST_SHARD
    public static final int COIN = 1012;      // GOLD_INGOT

    // Icônes de menu (objet de base PAPER)
    public static final int INVISIBLE = 2000;
    public static final int SHOP = 2001;
    public static final int WANTED = 2002;
    public static final int TOP = 2003;
    public static final int QUESTS = 2004;
    public static final int BACK = 2005;
    public static final int PREV = 2006;
    public static final int NEXT = 2007;
    public static final int CLOCK = 2008;
    public static final int LOCKED = 2009;
    public static final int CHECK = 2010;
    public static final int SELL = 2011;
    public static final int DEAL = 2012;

    private Icons() {
    }

    public static boolean enabled(BountySMP plugin) {
        return plugin.getConfig().getBoolean("resource-pack.enabled", true);
    }

    /** Applique le CustomModelData si le resource pack est activé dans la config. */
    public static void cmd(BountySMP plugin, ItemMeta meta, int cmd) {
        if (enabled(plugin)) {
            meta.setCustomModelData(cmd);
        }
    }

    /**
     * Icône de menu : papier + CustomModelData avec le pack, sinon l'objet vanilla de repli.
     */
    public static ItemStack gui(BountySMP plugin, int cmd, Material fallback, String name, List<String> lore) {
        boolean on = enabled(plugin);
        ItemStack item = new ItemStack(on ? Material.PAPER : fallback);
        ItemMeta meta = item.getItemMeta();
        if (on) {
            meta.setCustomModelData(cmd);
        }
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(l -> Component.text(l).decoration(TextDecoration.ITALIC, false)).toList());
        }
        item.setItemMeta(meta);
        return item;
    }
}
