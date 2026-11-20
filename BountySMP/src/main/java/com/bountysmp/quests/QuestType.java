package com.bountysmp.quests;

import org.bukkit.Material;

/** Types de quêtes quotidiennes. */
public enum QuestType {
    BREAK_LOGS("Abattre %d bûches", Material.OAK_LOG),
    KILL_MOBS("Tuer %d monstres", Material.IRON_SWORD),
    FISH("Pêcher %d poissons", Material.FISHING_ROD),
    PLAYTIME("Jouer %d minutes (actif)", Material.CLOCK),
    MINE_ORES("Miner %d minerais", Material.IRON_PICKAXE),
    MINE_DIAMOND("Miner %d diamants", Material.DIAMOND),
    KILL_PLAYERS("Tuer %d joueurs", Material.DIAMOND_SWORD),
    CLAIM_BOUNTY("Encaisser %d prime", Material.SKELETON_SKULL);

    private final String pattern;
    private final Material icon;

    QuestType(String pattern, Material icon) {
        this.pattern = pattern;
        this.icon = icon;
    }

    public String title(int target) {
        return String.format(pattern, target);
    }

    public Material icon() {
        return icon;
    }
}
