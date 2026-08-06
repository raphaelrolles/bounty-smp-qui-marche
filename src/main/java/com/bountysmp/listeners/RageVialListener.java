package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Gère la "Fiole de Rage" : objet combat/utilité qui donne Force + Résistance au feu.
 * Cooldown sur l'objet (comme la boussole/perle d'ender).
 */
public class RageVialListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey rageKey;

    public RageVialListener(BountySMP plugin) {
        this.plugin = plugin;
        this.rageKey = new NamespacedKey(plugin, "bounty_rage_vial");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(rageKey, PersistentDataType.BYTE);
        if (flag == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.hasCooldown(Material.GLOWSTONE_DUST)) {
            player.sendMessage(Component.text("La Fiole de Rage recharge encore...", NamedTextColor.GRAY));
            return;
        }

        int cooldownSeconds = plugin.getConfig().getInt("item-cooldowns.rage-vial-seconds", 25);
        player.setCooldown(Material.GLOWSTONE_DUST, cooldownSeconds * 20);

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 15, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 15, 0));
        player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, player.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.02);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        player.sendMessage(Component.text("🔥 Tu sens la rage monter en toi (Force + Résistance au feu, 15s).", NamedTextColor.GOLD));
    }
}
