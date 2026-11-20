package com.bountysmp.util;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Têtes de trophée : un joueur tué en PvP laisse sa tête, qu'on peut vendre avec /sell.
 * La valeur est écrite dans l'objet au moment du kill (elle ne change pas si la config change ensuite).
 */
public final class Trophy {

    private Trophy() {
    }

    private static NamespacedKey key(BountySMP plugin) {
        return new NamespacedKey(plugin, "bounty_trophy_value");
    }

    public static ItemStack create(BountySMP plugin, Player victim, Player killer, int value, boolean wanted) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(victim);
        meta.displayName(Component.text((wanted ? "§6☠ Tête recherchée de " : "§cTête de ") + victim.getName())
                .decoration(TextDecoration.ITALIC, false));
        List<String> lore = new ArrayList<>();
        lore.add("§7Tué par §f" + killer.getName());
        if (wanted) {
            lore.add("§6Prime encaissée !");
        }
        lore.add("§7Valeur : §e" + value + " coins");
        lore.add("§8(à vendre avec /sell)");
        meta.lore(lore.stream().map(l -> Component.text(l).decoration(TextDecoration.ITALIC, false)).toList());
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.INTEGER, value);
        head.setItemMeta(meta);
        return head;
    }

    /** Valeur en coins d'un objet, ou 0 si ce n'est pas une tête de trophée. */
    public static int valueOf(BountySMP plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Integer value = meta.getPersistentDataContainer().get(key(plugin), PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }
}
