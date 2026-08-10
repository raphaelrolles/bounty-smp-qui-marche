package com.bountysmp.util;

import com.bountysmp.BountySMP;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Enregistre et déclenche de VRAIS succès Minecraft (popup en haut à droite avec
 * icône, cadre et son vanilla), plutôt qu'un simple message ou titre à l'écran.
 */
public class AchievementUtil {

    private final BountySMP plugin;
    private final NamespacedKey firstPurchaseKey;
    private final NamespacedKey firstBountyKey;
    private final NamespacedKey seasonedHunterKey;

    public AchievementUtil(BountySMP plugin) {
        this.plugin = plugin;
        this.firstPurchaseKey = new NamespacedKey(plugin, "first_purchase");
        this.firstBountyKey = new NamespacedKey(plugin, "first_bounty");
        this.seasonedHunterKey = new NamespacedKey(plugin, "seasoned_hunter");
    }

    /** À appeler une fois au démarrage du plugin (onEnable), avant toute utilisation. */
    public void registerAdvancements() {
        register(firstPurchaseKey, Material.GOLD_INGOT,
                "Premier Achat", "Fais ton premier achat en boutique.", "task");
        register(firstBountyKey, Material.NETHERITE_SWORD,
                "Première Prime", "Encaisse ta première prime en éliminant un joueur recherché.", "goal");
        register(seasonedHunterKey, Material.DIAMOND_SWORD,
                "Chasseur Aguerri", "Encaisse 5 primes en éliminant des joueurs recherchés.", "challenge");
    }

    public void awardFirstPurchase(Player player) {
        award(player, firstPurchaseKey);
    }

    public void awardFirstBounty(Player player) {
        award(player, firstBountyKey);
    }

    public void awardSeasonedHunter(Player player) {
        award(player, seasonedHunterKey);
    }

    /**
     * Révoque la progression de TOUS les succès custom pour tous les joueurs actuellement
     * en ligne. À appeler en complément d'un /bounty reset, sinon le succès Minecraft reste
     * marqué "obtenu" côté client/serveur même si les données internes du plugin sont effacées,
     * et le joueur ne pourrait plus jamais retoucher le toast pour ce succès.
     */
    public void resetAllProgress() {
        List<NamespacedKey> keys = List.of(firstPurchaseKey, firstBountyKey, seasonedHunterKey);
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (NamespacedKey key : keys) {
                Advancement advancement = Bukkit.getAdvancement(key);
                if (advancement == null) continue;
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                for (String criteria : new ArrayList<>(progress.getAwardedCriteria())) {
                    progress.revokeCriteria(criteria);
                }
            }
        }
    }

    /**
     * Enregistre un succès personnalisé "caché" (n'apparaît pas dans l'arbre
     * d'avancement tant qu'il n'est pas obtenu) déclenché uniquement par le plugin.
     */
    private void register(NamespacedKey key, Material icon, String title, String description, String frame) {
        if (Bukkit.getAdvancement(key) != null) {
            return; // déjà enregistré (ex: reload du plugin)
        }
        String itemKey = icon.getKey().toString();
        String json = "{"
                + "\"criteria\":{\"trigger\":{\"trigger\":\"minecraft:impossible\"}},"
                + "\"display\":{"
                + "\"icon\":\"" + itemKey + "\","
                + "\"title\":{\"text\":\"" + escape(title) + "\"},"
                + "\"description\":{\"text\":\"" + escape(description) + "\"},"
                + "\"frame\":\"" + frame + "\","
                + "\"announce_to_chat\":false,"
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

    private void award(Player player, NamespacedKey key) {
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
