package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * Gère l'objet "Fumigène du Fugitif" acheté en boutique :
 * clic droit -> invisibilité + vitesse temporaires pour semer ses chasseurs.
 */
public class SmokeBombListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey smokeBombKey;

    public SmokeBombListener(BountySMP plugin) {
        this.plugin = plugin;
        this.smokeBombKey = plugin.getShopGUI().getSmokeBombKey();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(smokeBombKey, PersistentDataType.BYTE);
        if (flag == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 10, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 1));

        // Aveugle brièvement les joueurs proches (les chasseurs à ta poursuite) pour faciliter la fuite
        double radius = plugin.getConfig().getDouble("smoke-bomb.blind-radius", 6.0);
        for (org.bukkit.entity.Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof Player otherPlayer) {
                otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 0));
            }
        }

        player.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.02);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        player.sendMessage(Component.text("💨 Tu disparais dans la fumée pendant 10 secondes...", NamedTextColor.GRAY));
    }
}
