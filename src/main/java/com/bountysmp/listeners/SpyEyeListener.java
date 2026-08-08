package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
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

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gère l'"Œil de Judas" du Marché Noir : révèle la direction et la distance
 * approximative du joueur en ligne le plus proche (même monde).
 */
public class SpyEyeListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey spyKey;

    public SpyEyeListener(BountySMP plugin) {
        this.plugin = plugin;
        this.spyKey = new NamespacedKey(plugin, "bounty_spy_eye");
    }

    public NamespacedKey getSpyKey() {
        return spyKey;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(spyKey, PersistentDataType.BYTE);
        if (flag == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.hasCooldown(Material.ENDER_EYE)) {
            player.sendMessage(Component.text("L'Œil de Judas recharge encore...", NamedTextColor.GRAY));
            return;
        }

        Optional<Player> nearest = player.getWorld().getPlayers().stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(player.getLocation())));

        if (nearest.isEmpty()) {
            player.sendMessage(Component.text("Aucun autre joueur n'est présent dans ton monde.", NamedTextColor.GRAY));
            return;
        }

        int cooldownSeconds = plugin.getConfig().getInt("item-cooldowns.spy-eye-seconds", 20);
        player.setCooldown(Material.ENDER_EYE, cooldownSeconds * 20);

        Player target = nearest.get();
        Location from = player.getLocation();
        Location to = target.getLocation();

        double realDistance = from.distance(to);
        int imprecision = plugin.getConfig().getInt("spy-eye.imprecision-percent", 20);
        double jitterFactor = 1.0 + (ThreadLocalRandom.current().nextDouble(-imprecision, imprecision + 1) / 100.0);
        int approxDistance = Math.max(1, (int) Math.round(realDistance * jitterFactor));

        String direction = compassDirection(from, to);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        player.sendMessage(Component.text("👁 L'Œil de Judas frémit : quelqu'un se trouve ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("~" + approxDistance + " blocs", NamedTextColor.YELLOW))
                .append(Component.text(" au ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(direction, NamedTextColor.GOLD))
                .append(Component.text(".", NamedTextColor.LIGHT_PURPLE)));
    }

    private String compassDirection(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90;
        if (angle < 0) angle += 360;

        String[] directions = {"Sud", "Sud-Ouest", "Ouest", "Nord-Ouest", "Nord", "Nord-Est", "Est", "Sud-Est"};
        int index = (int) Math.round(angle / 45.0) % 8;
        return directions[index];
    }
}
