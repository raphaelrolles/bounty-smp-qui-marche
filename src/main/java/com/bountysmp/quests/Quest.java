package com.bountysmp.quests;

/** Une quête du jour : objectif et récompense en Bounty Coins. */
public record Quest(QuestType type, Tier tier, int target, int reward) {

    public enum Tier {
        EASY("§aFacile"), MEDIUM("§6Moyenne"), HARD("§cDifficile");

        private final String label;

        Tier(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Identifiant de stockage de la progression (un seul quête par type et par jour). */
    public String id() {
        return type.name();
    }

    public String title() {
        return type.title(target);
    }
}
