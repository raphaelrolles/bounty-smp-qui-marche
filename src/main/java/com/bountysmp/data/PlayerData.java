package com.bountysmp.data;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Représente toutes les données Bounty SMP d'un joueur.
 */
public class PlayerData {

    private final UUID uuid;

    private double coins = 0;
    private double bounty = 0;

    // Qui a mis combien sur la prime de ce joueur (nom du contributeur -> montant)
    private final Map<String, Double> bountyContributors = new LinkedHashMap<>();

    // Horodatage des kills récents de ce joueur, pour le système de danger automatique
    private final Deque<Long> recentKillTimestamps = new ArrayDeque<>();

    // Paliers de danger déjà déclenchés pendant la "série" en cours
    private final Set<Integer> triggeredThresholds = new HashSet<>();

    // Dernière fois que ce joueur a tué telle victime (anti-farm)
    private final Map<UUID, Long> lastKillOnVictim = new HashMap<>();

    // Nombre de primes récupérées (réputation)
    private int bountiesClaimed = 0;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getCoins() {
        return coins;
    }

    public void setCoins(double coins) {
        this.coins = Math.max(0, coins);
    }

    public void addCoins(double amount) {
        this.coins = Math.max(0, this.coins + amount);
    }

    public double getBounty() {
        return bounty;
    }

    public void setBounty(double bounty) {
        this.bounty = Math.max(0, bounty);
    }

    public void addBounty(double amount) {
        this.bounty = Math.max(0, this.bounty + amount);
    }

    public Map<String, Double> getBountyContributors() {
        return bountyContributors;
    }

    public void addContribution(String contributorName, double amount) {
        bountyContributors.merge(contributorName, amount, Double::sum);
        addBounty(amount);
    }

    public void clearBounty() {
        this.bounty = 0;
        this.bountyContributors.clear();
    }

    public Deque<Long> getRecentKillTimestamps() {
        return recentKillTimestamps;
    }

    public Set<Integer> getTriggeredThresholds() {
        return triggeredThresholds;
    }

    public Map<UUID, Long> getLastKillOnVictim() {
        return lastKillOnVictim;
    }

    public int getBountiesClaimed() {
        return bountiesClaimed;
    }

    public void incrementBountiesClaimed() {
        this.bountiesClaimed++;
    }

    public void setBountiesClaimed(int value) {
        this.bountiesClaimed = value;
    }
}
