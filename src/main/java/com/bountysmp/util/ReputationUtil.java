package com.bountysmp.util;

import com.bountysmp.BountySMP;
import org.bukkit.configuration.ConfigurationSection;

import java.util.TreeMap;

/**
 * Calcule le titre de réputation d'un joueur en fonction de son nombre de primes récupérées.
 */
public class ReputationUtil {

    private final TreeMap<Integer, String> titles = new TreeMap<>();

    public ReputationUtil(BountySMP plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("reputation");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    titles.put(Integer.parseInt(key), section.getString(key));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (titles.isEmpty()) {
            titles.put(0, "Aucun");
            titles.put(1, "Chasseur");
            titles.put(5, "Chasseur Confirme");
            titles.put(15, "Master Hunter");
        }
    }

    public String getTitle(int bountiesClaimed) {
        var entry = titles.floorEntry(bountiesClaimed);
        return entry != null ? entry.getValue() : "Aucun";
    }
}
