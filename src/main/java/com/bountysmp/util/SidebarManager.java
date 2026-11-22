package com.bountysmp.util;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scoreboard latéral personnel : chaque joueur choisit (depuis le menu /bounty)
 * entre "désactivé", "tableau des primes" et "tableau des chasseurs".
 */
public class SidebarManager implements Listener {

    public static final String OFF = "off";
    public static final String BOUNTIES = "bounties";
    public static final String HUNTERS = "hunters";

    private record Row(UUID id, String name, int value) {
    }

    private final BountySMP plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> lastRendered = new HashMap<>();

    public SidebarManager(BountySMP plugin) {
        this.plugin = plugin;
    }

    public static String label(String mode) {
        return switch (mode == null ? OFF : mode) {
            case BOUNTIES -> "Tableau des primes";
            case HUNTERS -> "Tableau des chasseurs";
            default -> "Désactivé";
        };
    }

    private static String next(String mode) {
        return switch (mode == null ? OFF : mode) {
            case OFF -> BOUNTIES;
            case BOUNTIES -> HUNTERS;
            default -> OFF;
        };
    }

    /** Passe au mode suivant (désactivé, primes, chasseurs) pour ce joueur. */
    public void cycle(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        data.setScoreboardMode(next(data.getScoreboardMode()));
        apply(player);
    }

    /** Affiche ou retire le scoreboard du joueur selon son mode. */
    public void apply(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        String mode = data.getScoreboardMode();
        UUID id = player.getUniqueId();
        if (OFF.equals(mode)) {
            boards.remove(id);
            lastRendered.remove(id);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            return;
        }
        Scoreboard board = boards.computeIfAbsent(id, k -> {
            Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();
            b.registerNewObjective("bounty", Criteria.DUMMY, Component.text("BOUNTY")).setDisplaySlot(DisplaySlot.SIDEBAR);
            return b;
        });
        player.setScoreboard(board);
        lastRendered.remove(id);
        render(player, board, mode, mode.equals(BOUNTIES) ? topBounties() : topHunters());
    }

    /** Appelé régulièrement : met à jour le scoreboard des joueurs qui l'ont activé. */
    public void tick() {
        List<Row> bounties = null;
        List<Row> hunters = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            String mode = plugin.getDataManager().get(player.getUniqueId()).getScoreboardMode();
            if (mode == null || OFF.equals(mode)) {
                continue;
            }
            Scoreboard board = boards.get(player.getUniqueId());
            if (board == null) {
                apply(player);
                continue;
            }
            if (player.getScoreboard() != board) {
                player.setScoreboard(board);
            }
            if (mode.equals(BOUNTIES)) {
                if (bounties == null) bounties = topBounties();
                render(player, board, mode, bounties);
            } else {
                if (hunters == null) hunters = topHunters();
                render(player, board, mode, hunters);
            }
        }
    }

    private void render(Player viewer, Scoreboard board, String mode, List<Row> rows) {
        Objective objective = board.getObjective("bounty");
        if (objective == null) return;
        objective.displayName(Component.text(mode.equals(BOUNTIES) ? "§6§l☠ PRIMES" : "§a§l⚔ CHASSEURS"));

        Map<String, Integer> target = new LinkedHashMap<>();
        int rank = 1;
        for (Row row : rows) {
            String color = rank == 1 ? "§6" : rank == 2 ? "§f" : rank == 3 ? "§c" : "§7";
            String entry = row.id().equals(viewer.getUniqueId()) ? "§e➤ " + row.name() : color + row.name();
            target.put(entry, row.value());
            rank++;
        }
        if (target.isEmpty()) {
            target.put("§7(personne)", 0);
        }
        if (target.equals(lastRendered.get(viewer.getUniqueId()))) {
            return; // rien n'a changé : on évite le clignotement
        }
        for (String entry : new ArrayList<>(board.getEntries())) {
            board.resetScores(entry);
        }
        target.forEach((entry, value) -> objective.getScore(entry).setScore(value));
        lastRendered.put(viewer.getUniqueId(), target);
    }

    private int maxLines() {
        return Math.max(3, Math.min(15, plugin.getConfig().getInt("scoreboard.lines", 8)));
    }

    private List<Row> topBounties() {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> e : plugin.getDataManager().all().entrySet()) {
            if (e.getValue().getBounty() <= 0) continue;
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            if (op.getName() == null || !Players.isReal(op)) continue;
            rows.add(new Row(e.getKey(), op.getName(), (int) e.getValue().getBounty()));
        }
        rows.sort(Comparator.comparingInt(Row::value).reversed());
        return rows.size() > maxLines() ? new ArrayList<>(rows.subList(0, maxLines())) : rows;
    }

    private List<Row> topHunters() {
        List<Map.Entry<UUID, PlayerData>> entries = new ArrayList<>(plugin.getDataManager().all().entrySet());
        entries.sort((a, b) -> {
            int c = Integer.compare(b.getValue().getBountiesClaimed(), a.getValue().getBountiesClaimed());
            return c != 0 ? c : Integer.compare(b.getValue().getKills(), a.getValue().getKills());
        });
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> e : entries) {
            if (e.getValue().getBountiesClaimed() <= 0) continue;
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            if (op.getName() == null || !Players.isReal(op)) continue;
            rows.add(new Row(e.getKey(), op.getName(), e.getValue().getBountiesClaimed()));
            if (rows.size() >= maxLines()) break;
        }
        return rows;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String mode = plugin.getDataManager().get(player.getUniqueId()).getScoreboardMode();
        if (mode != null && !OFF.equals(mode)) {
            apply(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boards.remove(event.getPlayer().getUniqueId());
        lastRendered.remove(event.getPlayer().getUniqueId());
    }
}
