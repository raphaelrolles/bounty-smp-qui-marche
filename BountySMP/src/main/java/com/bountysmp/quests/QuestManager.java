package com.bountysmp.quests;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import com.bountysmp.quests.Quest.Tier;
import com.bountysmp.util.Daily;
import com.bountysmp.util.Sfx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Génère 3 quêtes par jour (facile / moyenne / difficile), identiques pour tout le monde,
 * et suit la progression de chaque joueur. Les récompenses sont versées automatiquement.
 */
public class QuestManager {

    private static final List<QuestType> EASY = List.of(
            QuestType.BREAK_LOGS, QuestType.KILL_MOBS, QuestType.FISH, QuestType.PLAYTIME);
    private static final List<QuestType> MEDIUM = List.of(
            QuestType.MINE_ORES, QuestType.KILL_MOBS, QuestType.PLAYTIME, QuestType.FISH);
    private static final List<QuestType> HARD = List.of(
            QuestType.MINE_DIAMOND, QuestType.MINE_DIAMOND, QuestType.KILL_PLAYERS, QuestType.CLAIM_BOUNTY);

    private final BountySMP plugin;
    private long cachedDay = Long.MIN_VALUE;
    private int cachedSalt = 0;
    private List<Quest> cached = List.of();

    public QuestManager(BountySMP plugin) {
        this.plugin = plugin;
    }

    /** Les quêtes du jour (recalculées automatiquement à minuit). */
    public List<Quest> today() {
        long day = Daily.today(plugin);
        int salt = plugin.getDataManager().questSalt(day);
        if (day != cachedDay || salt != cachedSalt) {
            cached = generate(day, salt);
            cachedDay = day;
            cachedSalt = salt;
        }
        return cached;
    }

    private List<Quest> generate(long day, int salt) {
        Random rnd = new Random(day * 104729L + 17L + salt * 7919L);
        Set<QuestType> used = new HashSet<>();
        List<Quest> out = new ArrayList<>();
        out.add(pick(rnd, EASY, Tier.EASY, used));
        out.add(pick(rnd, MEDIUM, Tier.MEDIUM, used));
        out.add(pick(rnd, HARD, Tier.HARD, used));
        return List.copyOf(out);
    }

    private Quest pick(Random rnd, List<QuestType> pool, Tier tier, Set<QuestType> used) {
        List<QuestType> candidates = new ArrayList<>();
        for (QuestType t : pool) {
            if (!used.contains(t)) {
                candidates.add(t);
            }
        }
        QuestType type = candidates.get(rnd.nextInt(candidates.size()));
        used.add(type);
        int reward = switch (tier) {
            case EASY -> plugin.getConfig().getInt("quests.reward-easy", 30);
            case MEDIUM -> plugin.getConfig().getInt("quests.reward-medium", 60);
            case HARD -> plugin.getConfig().getInt("quests.reward-hard", 120);
        };
        return new Quest(type, tier, targetFor(type, tier), reward);
    }

    private int targetFor(QuestType type, Tier tier) {
        return switch (type) {
            case BREAK_LOGS -> 48;
            case KILL_MOBS -> tier == Tier.EASY ? 20 : 45;
            case FISH -> tier == Tier.EASY ? 8 : 20;
            case PLAYTIME -> tier == Tier.EASY ? 20 : 45;
            case MINE_ORES -> 40;
            case MINE_DIAMOND -> 8;
            case KILL_PLAYERS -> 2;
            case CLAIM_BOUNTY -> 1;
        };
    }

    /** Si le joueur n'a pas encore ses quêtes d'aujourd'hui, on remet sa progression à zéro. */
    public void ensureCurrent(PlayerData data) {
        long day = Daily.today(plugin);
        if (data.getQuestsEpochDay() != day) {
            data.resetQuests(day);
        }
    }

    public int progress(PlayerData data, Quest quest) {
        return data.getQuestProgress().getOrDefault(quest.id(), 0);
    }

    public boolean isDone(PlayerData data, Quest quest) {
        return data.getQuestsRewarded().contains(quest.id());
    }

    /** Ajoute de la progression à toute quête du jour correspondant au type donné. */
    public void add(Player player, QuestType type, int amount) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        ensureCurrent(data);
        for (Quest quest : today()) {
            if (quest.type() != type || isDone(data, quest)) {
                continue;
            }
            int updated = Math.min(quest.target(), progress(data, quest) + amount);
            data.getQuestProgress().put(quest.id(), updated);
            if (updated >= quest.target()) {
                complete(player, data, quest);
            }
        }
    }

    private void complete(Player player, PlayerData data, Quest quest) {
        if (!data.getQuestsRewarded().add(quest.id())) {
            return;
        }
        data.addCoins(quest.reward());
        data.addQuestsDone();
        player.sendActionBar(Component.text("✔ Quête terminée : ", NamedTextColor.GREEN)
                .append(Component.text(quest.title(), NamedTextColor.WHITE))
                .append(Component.text("  +" + quest.reward() + " coins", NamedTextColor.GOLD)));
        Sfx.questDone(plugin, player);

        boolean all = true;
        for (Quest q : today()) {
            if (!isDone(data, q)) {
                all = false;
                break;
            }
        }
        if (all && !data.isQuestBonusClaimed()) {
            data.setQuestBonusClaimed(true);
            plugin.getAchievementUtil().award(player, "quest_master");
            int bonus = plugin.getConfig().getInt("quests.bonus-reward", 50);
            data.addCoins(bonus);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendActionBar(Component.text("★ Toutes les quêtes du jour terminées ! Bonus : +" + bonus + " coins", NamedTextColor.GOLD));
                    Sfx.purseWin(plugin, player);
                }
            }, 60L);
        }
    }
}
