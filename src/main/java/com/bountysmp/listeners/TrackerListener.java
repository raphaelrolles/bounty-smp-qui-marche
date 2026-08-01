package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
import java.util.List;
import java.util.Optional;

/**
 * Gère l'objet "Tracker" acheté en boutique : clic droit -> pointe vers
 * le joueur recherché le plus proche (même monde).
 */
public class TrackerListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey trackerKey;

    public TrackerListener(BountySMP plugin) {
        this.plugin = plugin;
        this.trackerKey = new NamespacedKey(plugin, "bounty_tracker");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(trackerKey, PersistentDataType.BYTE);
        if (flag == null) return;

        Player player = event.getPlayer();

        List<PlayerData> wanted = plugin.getDataManager().topBounties(50);
        Optional<Player> nearest = wanted.stream()
                .map(d -> Bukkit.getPlayer(d.getUuid()))
                .filter(p -> p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())
                        && p.getWorld().equals(player.getWorld()))
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(player.getLocation())));

        if (nearest.isEmpty()) {
            player.sendMessage(Component.text("Aucun joueur recherché n'est actuellement présent dans ton monde.", NamedTextColor.GRAY));
            return;
        }

        Player target = nearest.get();
        player.setCompassTarget(target.getLocation());
        double distance = player.getLocation().distance(target.getLocation());
        player.sendMessage(Component.text("La boussole pointe maintenant vers ", NamedTextColor.GOLD)
                .append(Component.text(target.getName(), NamedTextColor.RED))
                .append(Component.text(String.format(" (~%.0f blocs)", distance), NamedTextColor.GRAY)));
    }
}
