package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.data.DataManager;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.TreeMap;

public class PlayerDeathListener implements Listener {

    private final BountySMP plugin;

    public PlayerDeathListener(BountySMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return; // pas de PvP direct (mort par mob, chute, etc.) -> pas de récompense
        }

        DataManager dm = plugin.getDataManager();
        PlayerData killerData = dm.get(killer.getUniqueId());
        PlayerData victimData = dm.get(victim.getUniqueId());

        // --- 1. Le tué avait-il une prime ? ---
        double bountyOnVictim = victimData.getBounty();
        if (bountyOnVictim > 0) {
            killerData.addCoins(bountyOnVictim);
            killerData.incrementBountiesClaimed();
            victimData.clearBounty();

            Bukkit.broadcast(Component.text("☠ ", NamedTextColor.DARK_RED)
                    .append(Component.text(killer.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" a éliminé le joueur recherché ", NamedTextColor.GRAY))
                    .append(Component.text(victim.getName(), NamedTextColor.RED))
                    .append(Component.text(" et remporte ", NamedTextColor.GRAY))
                    .append(Component.text((long) bountyOnVictim + " Bounty Coins !", NamedTextColor.YELLOW)));
        } else {
            // --- 2. Kill normal : récompense de base, avec anti-farm ---
            double reward = computeKillReward(killerData, victim.getUniqueId());
            killerData.addCoins(reward);
            killer.sendMessage(Component.text("+ " + (long) reward + " Bounty Coins ", NamedTextColor.GREEN)
                    .append(Component.text("(kill sur " + victim.getName() + ")", NamedTextColor.GRAY)));
        }

        // --- 3. Système de danger automatique ---
        applyDangerSystem(killer, killerData);

        killer.sendMessage(Component.text("Coins : ", NamedTextColor.GRAY)
                .append(Component.text((long) killerData.getCoins(), NamedTextColor.YELLOW)));
    }

    /**
     * Calcule la récompense de kill en tenant compte de l'anti-farm
     * (tuer plusieurs fois rapidement le même joueur réduit fortement le gain).
     */
    private double computeKillReward(PlayerData killerData, java.util.UUID victimUuid) {
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getLong("anti-farm.cooldown-seconds", 600) * 1000L;
        double baseReward = plugin.getConfig().getDouble("kill-reward", 5);
        double reducedReward = plugin.getConfig().getDouble("anti-farm.reduced-reward", 1);

        Map<java.util.UUID, Long> lastKills = killerData.getLastKillOnVictim();
        Long lastTime = lastKills.get(victimUuid);

        double reward;
        if (lastTime != null && (now - lastTime) < cooldownMs) {
            reward = reducedReward; // farm détecté -> récompense minimale
        } else {
            reward = baseReward;
        }
        lastKills.put(victimUuid, now);
        return reward;
    }

    /**
     * Augmente automatiquement la prime d'un joueur qui enchaîne les kills
     * dans la fenêtre de temps configurée (config.yml -> danger).
     */
    private void applyDangerSystem(Player killer, PlayerData killerData) {
        long now = System.currentTimeMillis();
        long windowMs = plugin.getConfig().getLong("danger.window-seconds", 600) * 1000L;

        var timestamps = killerData.getRecentKillTimestamps();
        timestamps.addLast(now);

        // Purge des kills trop vieux
        while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) > windowMs) {
            timestamps.pollFirst();
        }

        // Si la série est terminée depuis longtemps, on avait déjà purgé -> reset des paliers
        if (timestamps.size() <= 1) {
            killerData.getTriggeredThresholds().clear();
        }

        int killCount = timestamps.size();

        ConfigurationSection thresholds = plugin.getConfig().getConfigurationSection("danger.thresholds");
        if (thresholds == null) return;

        TreeMap<Integer, Double> sorted = new TreeMap<>();
        for (String key : thresholds.getKeys(false)) {
            try {
                sorted.put(Integer.parseInt(key), thresholds.getDouble(key));
            } catch (NumberFormatException ignored) {
            }
        }

        for (Map.Entry<Integer, Double> entry : sorted.entrySet()) {
            int threshold = entry.getKey();
            double bonus = entry.getValue();
            if (killCount >= threshold && !killerData.getTriggeredThresholds().contains(threshold)) {
                killerData.getTriggeredThresholds().add(threshold);
                killerData.addBounty(bonus);

                Bukkit.broadcast(Component.text("⚠ ", NamedTextColor.RED)
                        .append(Component.text(killer.getName(), NamedTextColor.GOLD))
                        .append(Component.text(" devient de plus en plus dangereux ! ", NamedTextColor.GRAY))
                        .append(Component.text("Prime +" + (long) bonus, NamedTextColor.YELLOW))
                        .append(Component.text(" (total : " + (long) killerData.getBounty() + " coins)", NamedTextColor.GRAY)));
            }
        }
    }
}
