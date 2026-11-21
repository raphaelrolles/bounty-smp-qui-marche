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

    // "Reroll" admin des quêtes / offres : change la graine du jour (valable jusqu'à minuit)
    private long dailyDay = Long.MIN_VALUE;
    private int questSalt = 0;
    private int dealSalt = 0;

    private void syncDailyDay(long today) {
        if (dailyDay != today) {
            dailyDay = today;
            questSalt = 0;
            dealSalt = 0;
        }
    }

    public int questSalt(long today) {
        syncDailyDay(today);
        return questSalt;
    }

    public int dealSalt(long today) {
        syncDailyDay(today);
        return dealSalt;
    }

    public void bumpQuestSalt(long today) {
        syncDailyDay(today);
        questSalt++;
    }

    public void bumpDealSalt(long today) {
        syncDailyDay(today);
        dealSalt++;
    }

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
        long savedDay = yaml.getLong("daily.day", -1L);
        dailyDay = savedDay < 0 ? Long.MIN_VALUE : savedDay;
        questSalt = yaml.getInt("daily.questSalt", 0);
        dealSalt = yaml.getInt("daily.dealSalt", 0);
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
                data.setFirstPurchaseAchievement(s.getBoolean("firstPurchaseAchievement", false));
                data.setFirstBountyAchievement(s.getBoolean("firstBountyAchievement", false));
                data.setDecoyUntil(s.getLong("decoyUntil", 0));
                data.setQuestsEpochDay(s.getLong("questsEpochDay", -1L));
                ConfigurationSection questSection = s.getConfigurationSection("questProgress");
                if (questSection != null) {
                    for (String qk : questSection.getKeys(false)) {
                        data.getQuestProgress().put(qk, questSection.getInt(qk));
                    }
                }
                data.getQuestsRewarded().addAll(s.getStringList("questsRewarded"));
                data.setQuestBonusClaimed(s.getBoolean("questBonusClaimed", false));
                data.setDealsEpochDay(s.getLong("dealsEpochDay", -1L));
                data.getDealsBought().addAll(s.getStringList("dealsBought"));
                data.setMenuTheme(s.getString("menuTheme", "pirate"));
                data.loadStats(s.getInt("kills", 0), s.getInt("deaths", 0), s.getDouble("highestBounty", 0),
                        s.getInt("trophiesSold", 0), s.getInt("pickpockets", 0), s.getInt("questsDone", 0));

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

    /** Efface TOUTES les données en mémoire et sur disque (coins, primes, achats, etc. de tous les joueurs). */
    public void resetAll() {
        cache.clear();
        dailyDay = Long.MIN_VALUE;
        questSalt = 0;
        dealSalt = 0;
        if (dataFile.exists()) {
            dataFile.delete();
        }
        logger.warning("BountySMP: toutes les données ont été réinitialisées via /bounty reset.");
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("daily.day", dailyDay == Long.MIN_VALUE ? -1L : dailyDay);
        yaml.set("daily.questSalt", questSalt);
        yaml.set("daily.dealSalt", dealSalt);
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
            yaml.set(path + ".firstPurchaseAchievement", data.isFirstPurchaseAchievement());
            yaml.set(path + ".firstBountyAchievement", data.isFirstBountyAchievement());
            yaml.set(path + ".decoyUntil", data.getDecoyUntil());
            yaml.set(path + ".questsEpochDay", data.getQuestsEpochDay());
            for (Map.Entry<String, Integer> qe : data.getQuestProgress().entrySet()) {
                yaml.set(path + ".questProgress." + qe.getKey(), qe.getValue());
            }
            yaml.set(path + ".questsRewarded", new java.util.ArrayList<>(data.getQuestsRewarded()));
            yaml.set(path + ".questBonusClaimed", data.isQuestBonusClaimed());
            yaml.set(path + ".dealsEpochDay", data.getDealsEpochDay());
            yaml.set(path + ".dealsBought", new java.util.ArrayList<>(data.getDealsBought()));
            yaml.set(path + ".menuTheme", data.getMenuTheme());
            yaml.set(path + ".kills", data.getKills());
            yaml.set(path + ".deaths", data.getDeaths());
            yaml.set(path + ".highestBounty", data.getHighestBounty());
            yaml.set(path + ".trophiesSold", data.getTrophiesSold());
            yaml.set(path + ".pickpockets", data.getPickpockets());
            yaml.set(path + ".questsDone", data.getQuestsDone());
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
