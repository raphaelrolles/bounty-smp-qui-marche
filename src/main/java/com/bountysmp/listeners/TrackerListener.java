package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gère l'objet "Tracker" acheté en boutique :
 * - Clic droit + Maj (sneak) : ouvre le menu /wanted pour choisir sa cible.
 * - Clic droit simple : donne des coordonnées approximatives de la cible (avec cooldown).
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

        event.setCancelled(true);
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        // Clic droit + Maj : (ré)ouvrir le sélecteur de cible
        if (player.isSneaking() || data.getTrackerTarget() == null) {
            plugin.getWantedGUI().open(player);
            return;
        }

        pingTarget(player, data);
    }

    private void pingTarget(Player player, PlayerData data) {
        UUID targetUuid = data.getTrackerTarget();
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() != null ? target.getName() : "ta cible";

        Player onlineTarget = Bukkit.getPlayer(targetUuid);
        if (onlineTarget == null || !onlineTarget.isOnline()) {
            player.sendMessage(Component.text(targetName + " n'est plus en ligne. Reclique en Maj pour choisir une nouvelle cible.", NamedTextColor.GRAY));
            return;
        }

        long now = System.currentTimeMillis();
        if (now < data.getTrackerCooldownUntil()) {
            long remaining = (data.getTrackerCooldownUntil() - now) / 1000L + 1;
            player.sendMessage(Component.text("Le Tracker recharge encore " + remaining + "s...", NamedTextColor.GRAY));
            return;
        }

        int cooldownSeconds = plugin.getConfig().getInt("tracker.cooldown-seconds", 30);
        int imprecision = plugin.getConfig().getInt("tracker.imprecision-blocks", 40);
        data.setTrackerCooldownUntil(now + cooldownSeconds * 1000L);

        Location loc = onlineTarget.getLocation();
        int jitterX = ThreadLocalRandom.current().nextInt(-imprecision, imprecision + 1);
        int jitterZ = ThreadLocalRandom.current().nextInt(-imprecision, imprecision + 1);
        int approxX = (int) (Math.round((loc.getX() + jitterX) / 10.0) * 10);
        int approxZ = (int) (Math.round((loc.getZ() + jitterZ) / 10.0) * 10);

        player.sendMessage(Component.text("📡 Signal du Tracker sur ", NamedTextColor.GOLD)
                .append(Component.text(targetName, NamedTextColor.RED))
                .append(Component.text(" (" + loc.getWorld().getName() + ") : ", NamedTextColor.GRAY))
                .append(Component.text("X ≈ " + approxX + ", Z ≈ " + approxZ, NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("(coordonnées imprécises — prochain signal dans " + cooldownSeconds + "s)", NamedTextColor.DARK_GRAY));
    }
}
