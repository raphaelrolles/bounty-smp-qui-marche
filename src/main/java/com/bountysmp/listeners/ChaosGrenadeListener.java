package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gère la "Grenade du Chaos" : un objet de combat qui se lance comme une boule
 * de neige et applique un effet négatif aléatoire au joueur touché.
 */
public class ChaosGrenadeListener implements Listener {

    private static final String METADATA_KEY = "bounty_chaos_grenade";
    private static final List<PotionEffectType> EFFECTS = List.of(
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.POISON,
            PotionEffectType.NAUSEA,
            PotionEffectType.BLINDNESS,
            PotionEffectType.MINING_FATIGUE
    );

    private final BountySMP plugin;
    private final NamespacedKey chaosKey;

    public ChaosGrenadeListener(BountySMP plugin) {
        this.plugin = plugin;
        this.chaosKey = new NamespacedKey(plugin, "bounty_chaos_grenade");
    }

    public NamespacedKey getChaosKey() {
        return chaosKey;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(chaosKey, PersistentDataType.BYTE);
        if (flag == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.hasCooldown(Material.SNOWBALL)) {
            player.sendMessage(Component.text("La Grenade du Chaos recharge encore...", NamedTextColor.GRAY));
            return;
        }
        int cooldownSeconds = plugin.getConfig().getInt("item-cooldowns.chaos-grenade-seconds", 20);
        player.setCooldown(Material.SNOWBALL, cooldownSeconds * 20);

        Snowball snowball = player.launchProjectile(Snowball.class, player.getLocation().getDirection().multiply(1.4));
        snowball.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        player.sendMessage(Component.text("💥 Grenade du Chaos lancée !", NamedTextColor.LIGHT_PURPLE));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!event.getEntity().hasMetadata(METADATA_KEY)) return;
        if (!(event.getHitEntity() instanceof LivingEntity target)) return;

        PotionEffectType effect = EFFECTS.get(ThreadLocalRandom.current().nextInt(EFFECTS.size()));
        target.addPotionEffect(new PotionEffect(effect, 20 * 5, 1));
        target.getWorld().spawnParticle(org.bukkit.Particle.WITCH, target.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.05);

        if (target instanceof Player targetPlayer) {
            targetPlayer.sendMessage(Component.text("💥 Tu es touché par une Grenade du Chaos : ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(effect.getKey().getKey(), NamedTextColor.DARK_PURPLE)));
        }
    }
}
