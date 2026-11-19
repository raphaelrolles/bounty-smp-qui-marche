package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.util.Sfx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

/**
 * Marchand Pirate : un villageois immobile et invulnérable qu'un admin place où il veut
 * (/bounty merchant spawn). Un clic droit dessus ouvre la boutique.
 */
public class MerchantListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey merchantKey;

    public MerchantListener(BountySMP plugin) {
        this.plugin = plugin;
        this.merchantKey = new NamespacedKey(plugin, "bounty_merchant");
    }

    public boolean isMerchant(Entity entity) {
        return entity instanceof Villager
                && entity.getPersistentDataContainer().has(merchantKey, PersistentDataType.BYTE);
    }

    /** Fait apparaître un marchand à cet endroit, tourné vers le joueur qui le place. */
    public Villager spawn(Location where) {
        Location loc = where.clone();
        loc.setYaw(where.getYaw() + 180f);
        loc.setPitch(0f);
        return loc.getWorld().spawn(loc, Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setProfession(Villager.Profession.FISHERMAN);
            villager.customName(Component.text("Marchand Pirate", NamedTextColor.GOLD));
            villager.setCustomNameVisible(true);
            villager.getPersistentDataContainer().set(merchantKey, PersistentDataType.BYTE, (byte) 1);
        });
    }

    /** Supprime le marchand le plus proche du joueur (dans le rayon donné). */
    public boolean removeNearest(Player player, double radius) {
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (isMerchant(e)) {
                double d = e.getLocation().distanceSquared(player.getLocation());
                if (d < bestDistance) {
                    bestDistance = d;
                    best = e;
                }
            }
        }
        if (best == null) {
            return false;
        }
        best.remove();
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!isMerchant(event.getRightClicked())) return;
        event.setCancelled(true); // pas de fenêtre d'échange vanilla
        if (event.getHand() != EquipmentSlot.HAND) return; // évite le double déclenchement (une fois par main)
        Player player = event.getPlayer();
        Sfx.click(player);
        plugin.getShopGUI().open(player);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (isMerchant(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTransform(EntityTransformEvent event) {
        if (isMerchant(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
