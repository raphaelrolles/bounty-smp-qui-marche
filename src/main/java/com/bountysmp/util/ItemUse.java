package com.bountysmp.util;

import com.bountysmp.BountySMP;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Locale;

/** Utilitaires communs pour l'utilisation des items custom (main principale ET main secondaire). */
public final class ItemUse {

    private ItemUse() {
    }

    /**
     * Retire 1 exemplaire de l'objet tenu dans la main qui a servi à l'utiliser.
     * (PlayerInventory#removeItem ne regarde pas la main secondaire : d'où l'objet infini en off-hand.)
     */
    public static void consumeOne(Player player, EquipmentSlot hand) {
        PlayerInventory inv = player.getInventory();
        boolean off = hand == EquipmentSlot.OFF_HAND;
        ItemStack stack = off ? inv.getItemInOffHand() : inv.getItemInMainHand();
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        ItemStack remaining;
        if (stack.getAmount() > 1) {
            remaining = stack.clone();
            remaining.setAmount(stack.getAmount() - 1);
        } else {
            remaining = new ItemStack(Material.AIR);
        }
        if (off) {
            inv.setItemInOffHand(remaining);
        } else {
            inv.setItemInMainHand(remaining);
        }
    }

    /** Vrai si l'objet est un item utilisable du plugin (porte une clé interne du plugin). */
    private static boolean isUsableCustom(BountySMP plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String namespace = plugin.getName().toLowerCase(Locale.ROOT);
        for (NamespacedKey key : item.getItemMeta().getPersistentDataContainer().getKeys()) {
            if (key.getNamespace().equals(namespace) && !key.getKey().startsWith("bounty_fake_ingot")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Quand on tient un item custom dans chaque main, un clic droit déclenche l'événement pour les deux mains.
     * La main principale est prioritaire : on neutralise alors l'événement de la main secondaire.
     *
     * @return vrai si l'événement doit être ignoré par le listener.
     */
    public static boolean guardOffhand(BountySMP plugin, PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.OFF_HAND) {
            return false;
        }
        if (isUsableCustom(plugin, event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }
}
