package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Egg;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Gère la "Bombe Puante" du Marché Noir : se lance et applique Nausée + Poison
 * au joueur touché.
 */
public class StinkBombListener implements Listener {

    private static final String METADATA_KEY = "bounty_stink_bomb";

    private final BountySMP plugin;
    private final NamespacedKey stinkKey;

    public StinkBombListener(BountySMP plugin) {
        this.plugin = plugin;
        this.stinkKey = new NamespacedKey(plugin, "bounty_stink_bomb");
    }

    public NamespacedKey getStinkKey() {
        return stinkKey;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(stinkKey, PersistentDataType.BYTE);
        if (flag == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.hasCooldown(Material.FERMENTED_SPIDER_EYE)) {
            player.sendMessage(Component.text("La Bombe Puante recharge encore...", NamedTextColor.GRAY));
            return;
        }
        int cooldownSeconds = plugin.getConfig().getInt("item-cooldowns.stink-bomb-seconds", 12);
        player.setCooldown(Material.FERMENTED_SPIDER_EYE, cooldownSeconds * 20);

        Egg egg = player.launchProjectile(Egg.class, player.getLocation().getDirection().multiply(1.4));
        egg.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        player.sendMessage(Component.text("🤢 Bombe Puante lancée !", NamedTextColor.DARK_GREEN));
    }

    @EventHandler
    public void onEggThrow(PlayerEggThrowEvent event) {
        if (event.getEgg().hasMetadata(METADATA_KEY)) {
            event.setHatching(false);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!event.getEntity().hasMetadata(METADATA_KEY)) return;
        if (!(event.getHitEntity() instanceof LivingEntity target)) return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 10, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 5, 0));
        target.getWorld().spawnParticle(org.bukkit.Particle.ITEM_SLIME, target.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.05);

        if (target instanceof Player targetPlayer) {
            targetPlayer.sendMessage(Component.text("🤢 Une Bombe Puante t'a touché... Beurk.", NamedTextColor.DARK_GREEN));
        }
    }
}
