package com.bountysmp.util;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Vrais succès Minecraft (popup + chat) déclenchés par le plugin. */
public class AchievementUtil {

    private record Ach(String id, Material icon, String title, String description, String frame) {
    }

    private static final List<Ach> ALL = List.of(
            new Ach("first_purchase", Material.GOLD_INGOT, "Premier Achat", "Fais ton premier achat en boutique.", "task"),
            new Ach("first_bounty", Material.NETHERITE_SWORD, "Première Prime", "Encaisse ta première prime en éliminant un joueur recherché.", "goal"),
            new Ach("seasoned_hunter", Material.DIAMOND_SWORD, "Chasseur Aguerri", "Encaisse 5 primes en éliminant des joueurs recherchés.", "challenge"),
            new Ach("first_blood", Material.IRON_SWORD, "Premier Sang", "Élimine un autre joueur.", "task"),
            new Ach("serial_killer", Material.NETHERITE_AXE, "Tueur en Série", "Élimine 10 joueurs.", "goal"),
            new Ach("legend", Material.NETHER_STAR, "Chasseur de Légende", "Encaisse 25 primes.", "challenge"),
            new Ach("millionaire", Material.EMERALD_BLOCK, "Millionnaire", "Possède 1000 Bounty Coins en même temps.", "goal"),
            new Ach("thief", Material.STRING, "Voleur", "Réussis ton premier vol avec une Bourse Trouée.", "task"),
            new Ach("wanted_first", Material.SKELETON_SKULL, "Sur la Liste", "Deviens recherché : quelqu'un a mis ta tête à prix.", "task"),
            new Ach("big_target", Material.WITHER_SKELETON_SKULL, "Tête Mise à Prix", "Atteins une prime de 500 coins sur ta tête.", "goal"),
            new Ach("collector", Material.PLAYER_HEAD, "Collectionneur", "Vends une tête de trophée avec /sell.", "task"),
            new Ach("deal_hunter", Material.EMERALD, "Bon Plan", "Achète une offre du jour.", "task"),
            new Ach("quest_master", Material.WRITABLE_BOOK, "Assidu", "Termine les 3 quêtes du jour.", "goal"));

    private final BountySMP plugin;
    private final Map<String, NamespacedKey> keys = new LinkedHashMap<>();

    public AchievementUtil(BountySMP plugin) {
        this.plugin = plugin;
        for (Ach a : ALL) {
            keys.put(a.id(), new NamespacedKey(plugin, a.id()));
        }
    }

    /** À appeler une fois au démarrage (onEnable). */
    public void registerAdvancements() {
        for (Ach a : ALL) {
            register(keys.get(a.id()), a);
        }
    }

    public int total() {
        return ALL.size();
    }

    public int countDone(Player player) {
        int n = 0;
        for (NamespacedKey key : keys.values()) {
            Advancement adv = Bukkit.getAdvancement(key);
            if (adv != null && player.getAdvancementProgress(adv).isDone()) {
                n++;
            }
        }
        return n;
    }

    public void awardFirstPurchase(Player player) {
        award(player, "first_purchase");
    }

    public void awardFirstBounty(Player player) {
        award(player, "first_bounty");
    }

    public void awardSeasonedHunter(Player player) {
        award(player, "seasoned_hunter");
    }

    /** Vérifie les succès "à seuil" (solde, prime) ; appelé régulièrement pour les joueurs connectés. */
    public void checkThresholds(Player player) {
        PlayerData d = plugin.getDataManager().get(player.getUniqueId());
        d.updateHighestBounty(d.getBounty());
        if (d.getCoins() >= 1000) award(player, "millionaire");
        if (d.getBounty() > 0) award(player, "wanted_first");
        if (d.getBounty() >= 500) award(player, "big_target");
    }

    /** Révoque tous les succès custom des joueurs connectés (utilisé par /bounty reset). */
    public void resetAllProgress() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (NamespacedKey key : keys.values()) {
                Advancement advancement = Bukkit.getAdvancement(key);
                if (advancement == null) continue;
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                for (String criteria : new ArrayList<>(progress.getAwardedCriteria())) {
                    progress.revokeCriteria(criteria);
                }
            }
        }
    }

    private void register(NamespacedKey key, Ach a) {
        if (Bukkit.getAdvancement(key) != null) {
            return;
        }
        String json = "{"
                + "\"criteria\":{\"trigger\":{\"trigger\":\"minecraft:impossible\"}},"
                + "\"display\":{"
                + "\"icon\":\"" + a.icon().getKey() + "\","
                + "\"title\":{\"text\":\"" + escape(a.title()) + "\"},"
                + "\"description\":{\"text\":\"" + escape(a.description()) + "\"},"
                + "\"frame\":\"" + a.frame() + "\","
                + "\"announce_to_chat\":true,"
                + "\"show_toast\":true,"
                + "\"hidden\":true"
                + "}"
                + "}";
        try {
            Bukkit.getUnsafe().loadAdvancement(key, json);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Impossible de charger le succès " + key + " : " + e.getMessage());
        }
    }

    public void award(Player player, String id) {
        NamespacedKey key = keys.get(id);
        if (key == null) return;
        Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (progress.isDone()) return;
        for (String criteria : progress.getRemainingCriteria()) {
            progress.awardCriteria(criteria);
        }
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
