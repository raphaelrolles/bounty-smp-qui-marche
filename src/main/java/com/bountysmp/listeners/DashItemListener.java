package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.util.Vector;

/**
 * Gère l'objet "Élan du Chasseur" : propulse le joueur en avant (dash) pour
 * rattraper une cible ou s'échapper, avec une brève Chute Ralentie pour éviter les dégâts.
 */
public class DashItemListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey dashKey;

    public DashItemListener(BountySMP plugin) {
        this.plugin = plugin;
        this.dashKey = new NamespacedKey(plugin, "bounty_dash_item");
    }

    public NamespacedKey getDashKey() {
        return dashKey;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(dashKey, PersistentDataType.BYTE);
        if (flag == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.hasCooldown(Material.FEATHER)) {
            player.sendMessage(Component.text("L'Élan du Chasseur recharge encore...", NamedTextColor.GRAY));
            return;
        }
        int cooldownSeconds = plugin.getConfig().getInt("item-cooldowns.dash-item-seconds", 20);
        player.setCooldown(Material.FEATHER, cooldownSeconds * 20);

        double strength = plugin.getConfig().getDouble("dash-item.strength", 2.2);
        Vector direction = player.getLocation().getDirection().normalize().multiply(strength);
        direction.setY(Math.max(direction.getY(), 0.35));
        player.setVelocity(direction);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 3, 0));

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.6f);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        player.sendMessage(Component.text("💨 Élan !", NamedTextColor.AQUA));
    }
}
