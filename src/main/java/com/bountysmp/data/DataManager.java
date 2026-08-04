package com.bountysmp.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Gère le chargement/la sauvegarde de toutes les PlayerData dans plugins/BountySMP/data.yml
 */
public class DataManager {

    private final File dataFile;
    private final Logger logger;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataManager(File dataFolder, Logger logger) {
        this.logger = logger;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "data.yml");
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerData::new);
    }

    public Map<UUID, PlayerData> all() {
        return cache;
    }

    /** Classement des primes, du plus recherché au moins recherché. */
    public List<PlayerData> topBounties(int limit) {
        return cache.values().stream()
                .filter(d -> d.getBounty() > 0)
                .sorted(Comparator.comparingDouble(PlayerData::getBounty).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** Classement des meilleurs chasseurs (primes récupérées). */
    public List<PlayerData> topHunters(int limit) {
        return cache.values().stream()
                .filter(d -> d.getBountiesClaimed() > 0)
                .sorted(Comparator.comparingInt(PlayerData::getBountiesClaimed).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void load() {
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }
        for (String key : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection s = playersSection.getConfigurationSection(key);
                if (s == null) continue;

                PlayerData data = new PlayerData(uuid);
                data.setCoins(s.getDouble("coins", 0));
                data.setBounty(s.getDouble("bounty", 0));
                data.setBountiesClaimed(s.getInt("bountiesClaimed", 0));
                data.getOneTimePurchases().addAll(s.getStringList("oneTimePurchases"));
                String trackerTargetStr = s.getString("trackerTarget", null);
                if (trackerTargetStr != null) {
                    try {
                        data.setTrackerTarget(UUID.fromString(trackerTargetStr));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                data.setTrackerCooldownUntil(s.getLong("trackerCooldownUntil", 0));
                data.setOreExchangesToday(s.getInt("oreExchangesToday", 0));
                data.setOreExchangeEpochDay(s.getLong("oreExchangeEpochDay", -1L));

                ConfigurationSection contribSection = s.getConfigurationSection("contributors");
                if (contribSection != null) {
                    for (String contributor : contribSection.getKeys(false)) {
                        data.getBountyContributors().put(contributor, contribSection.getDouble(contributor));
                    }
                }

                List<Long> timestamps = s.getLongList("recentKills");
                data.getRecentKillTimestamps().addAll(new ArrayDeque<>(timestamps));

                cache.put(uuid, data);
            } catch (IllegalArgumentException ex) {
                logger.warning("Entrée data.yml invalide ignorée: " + key);
            }
        }
        logger.info("BountySMP: " + cache.size() + " profils joueurs chargés.");
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            PlayerData data = entry.getValue();
            String path = "players." + entry.getKey();
            yaml.set(path + ".coins", data.getCoins());
            yaml.set(path + ".bounty", data.getBounty());
            yaml.set(path + ".bountiesClaimed", data.getBountiesClaimed());
            yaml.set(path + ".oneTimePurchases", new java.util.ArrayList<>(data.getOneTimePurchases()));
            if (data.getTrackerTarget() != null) {
                yaml.set(path + ".trackerTarget", data.getTrackerTarget().toString());
            }
            yaml.set(path + ".trackerCooldownUntil", data.getTrackerCooldownUntil());
            yaml.set(path + ".oreExchangesToday", data.getOreExchangesToday());
            yaml.set(path + ".oreExchangeEpochDay", data.getOreExchangeEpochDay());
            for (Map.Entry<String, Double> c : data.getBountyContributors().entrySet()) {
                yaml.set(path + ".contributors." + c.getKey(), c.getValue());
            }
            yaml.set(path + ".recentKills", new java.util.ArrayList<>(data.getRecentKillTimestamps()));
        }
        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Impossible de sauvegarder data.yml", e);
        }
    }
}
