package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Gère le "Faux Lingot d'Or" du Marché Noir : un piège à laisser traîner.
 * Quiconque (autre que le propriétaire) le ramasse est maudit temporairement.
 */
public class FakeIngotListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey fakeIngotKey;
    private final NamespacedKey ownerKey;

    public FakeIngotListener(BountySMP plugin) {
        this.plugin = plugin;
        this.fakeIngotKey = new NamespacedKey(plugin, "bounty_fake_ingot");
        this.ownerKey = new NamespacedKey(plugin, "bounty_fake_ingot_owner");
    }

    public NamespacedKey getFakeIngotKey() {
        return fakeIngotKey;
    }

    public NamespacedKey getOwnerKey() {
        return ownerKey;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player picker)) return;

        ItemStack stack = event.getItem().getItemStack();
        if (stack.getItemMeta() == null) return;
        Byte flag = stack.getItemMeta().getPersistentDataContainer().get(fakeIngotKey, PersistentDataType.BYTE);
        if (flag == null) return;

        String ownerStr = stack.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (ownerStr != null && ownerStr.equals(picker.getUniqueId().toString())) {
            return; // le propriétaire peut le reprendre sans risque
        }

        event.setCancelled(true);
        event.getItem().remove();

        picker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 10, 1));
        picker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 1));
        picker.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, picker.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
        picker.sendMessage(Component.text("💰 Ce lingot était un piège du Marché Noir... Tu es maudit !", NamedTextColor.DARK_RED));
    }
}
