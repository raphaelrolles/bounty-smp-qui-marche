package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.quests.QuestType;
import com.bountysmp.util.Sfx;
import com.bountysmp.util.Trophy;
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
        boolean farmKill = isRecentKill(killerData, victim.getUniqueId());
        plugin.getQuestManager().add(killer, QuestType.KILL_PLAYERS, 1);
        killerData.addKills();
        victimData.addDeaths();
        victimData.updateHighestBounty(victimData.getBounty());
        if (killerData.getKills() >= 1) plugin.getAchievementUtil().award(killer, "first_blood");
        if (killerData.getKills() >= 10) plugin.getAchievementUtil().award(killer, "serial_killer");
        Component gain;
        if (bountyOnVictim > 0) {
            killerData.addCoins(bountyOnVictim);
            killerData.incrementBountiesClaimed();
            victimData.clearBounty();
            gain = Component.text("☠ Prime encaissée : +" + (long) bountyOnVictim + " coins", NamedTextColor.GOLD);
            Sfx.bountyClaimed(plugin, killer);
            plugin.getQuestManager().add(killer, QuestType.CLAIM_BOUNTY, 1);

            if (!killerData.isFirstBountyAchievement()) {
                killerData.setFirstBountyAchievement(true);
                plugin.getAchievementUtil().awardFirstBounty(killer);
            }
            if (killerData.getBountiesClaimed() == 5) {
                plugin.getAchievementUtil().awardSeasonedHunter(killer);
            }
            if (killerData.getBountiesClaimed() >= 25) {
                plugin.getAchievementUtil().award(killer, "legend");
            }

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
            gain = Component.text("+" + (long) reward + " coins ", NamedTextColor.GREEN)
                    .append(Component.text("(kill sur " + victim.getName() + ")", NamedTextColor.GRAY));
            Sfx.killReward(killer);

            // Danger automatique : uniquement pour les kills NORMAUX (voir ci-dessous)
            applyDangerSystem(killer, killerData);
        }

        // Tuer un joueur RECHERCHÉ ne fait pas monter ta propre prime : le système de danger ne s'applique
        // qu'aux kills normaux, sinon plus personne n'oserait chasser les primes.
        killerData.getLastKillOnVictim().put(victim.getUniqueId(), System.currentTimeMillis());

        // --- 3. Tête de trophée ---
        dropTrophy(event, victim, killer, bountyOnVictim > 0, farmKill);

        killer.sendActionBar(gain
                .append(Component.text("  •  Solde : ", NamedTextColor.DARK_GRAY))
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
                Sfx.danger();
            }
        }
    }

    /** Vrai si le tueur a déjà tué ce joueur il y a peu de temps (même fenêtre que l'anti-farm). */
    private boolean isRecentKill(PlayerData killerData, java.util.UUID victimUuid) {
        long cooldownMs = plugin.getConfig().getLong("anti-farm.cooldown-seconds", 600) * 1000L;
        Long last = killerData.getLastKillOnVictim().get(victimUuid);
        return last != null && (System.currentTimeMillis() - last) < cooldownMs;
    }

    /** La victime laisse sa tête : valeur normale, plus élevée si elle était recherchée, minimale si c'est du farm. */
    private void dropTrophy(PlayerDeathEvent event, Player victim, Player killer, boolean wanted, boolean farm) {
        if (!plugin.getConfig().getBoolean("trophy.enabled", true)) {
            return;
        }
        int value;
        if (farm) {
            value = plugin.getConfig().getInt("trophy.farm-head-value", 1);
        } else if (wanted) {
            value = plugin.getConfig().getInt("trophy.wanted-head-value", 75);
        } else {
            value = plugin.getConfig().getInt("trophy.head-value", 25);
        }
        event.getDrops().add(Trophy.create(plugin, victim, killer, value, wanted));
    }
}
