package com.bountysmp.quests;

import com.bountysmp.BountySMP;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Écoute le jeu vanilla pour faire avancer les quêtes quotidiennes. */
public class QuestListener implements Listener {

    private final BountySMP plugin;
    private final QuestManager quests;
    private final Map<UUID, Location> lastSeen = new HashMap<>();

    public QuestListener(BountySMP plugin, QuestManager quests) {
        this.plugin = plugin;
        this.quests = quests;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material m = event.getBlock().getType();
        if (Tag.LOGS.isTagged(m)) {
            quests.add(player, QuestType.BREAK_LOGS, 1);
        }
        if (m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS) {
            quests.add(player, QuestType.MINE_ORES, 1);
        }
        if (m == Material.DIAMOND_ORE || m == Material.DEEPSLATE_DIAMOND_ORE) {
            quests.add(player, QuestType.MINE_DIAMOND, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && event.getEntity() instanceof Enemy) {
            quests.add(killer, QuestType.KILL_MOBS, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            quests.add(event.getPlayer(), QuestType.FISH, 1);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastSeen.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Appelé toutes les minutes : +1 minute de jeu pour les joueurs qui ont bougé
     * ou regardé ailleurs depuis la dernière fois (anti-AFK).
     */
    public void tickPlaytime() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location now = player.getLocation();
            Location before = lastSeen.put(player.getUniqueId(), now.clone());
            if (before == null || before.getWorld() != now.getWorld()) {
                continue;
            }
            double dx = now.getX() - before.getX();
            double dz = now.getZ() - before.getZ();
            double dYaw = Math.abs(now.getYaw() - before.getYaw());
            if (dx * dx + dz * dz > 1.0 || dYaw > 10.0) {
                quests.add(player, QuestType.PLAYTIME, 1);
            }
        }
    }
}
