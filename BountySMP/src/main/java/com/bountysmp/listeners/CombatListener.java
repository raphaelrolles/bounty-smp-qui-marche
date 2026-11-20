package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.util.Sfx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Combat tag (se déconnecter en combat = mort), zone sans PvP autour du spawn du monde,
 * et protection PvP des tout nouveaux joueurs (première session).
 */
public class CombatListener implements Listener {

    private final BountySMP plugin;
    private final Map<UUID, Long> tagUntil = new HashMap<>();
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private final Map<UUID, Long> protectedUntil = new HashMap<>();

    public CombatListener(BountySMP plugin) {
        this.plugin = plugin;
    }

    public boolean isTagged(Player player) {
        Long until = tagUntil.get(player.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private boolean isProtected(Player player) {
        Long until = protectedUntil.get(player.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private boolean inSpawnZone(Player player) {
        if (!plugin.getConfig().getBoolean("spawn-protection.enabled", true)) return false;
        World main = Bukkit.getWorlds().get(0);
        if (!player.getWorld().equals(main)) return false;
        Location spawn = main.getSpawnLocation();
        double r = plugin.getConfig().getDouble("spawn-protection.radius", 40.0);
        double dx = player.getLocation().getX() - spawn.getX();
        double dz = player.getLocation().getZ() - spawn.getZ();
        return dx * dx + dz * dz <= r * r;
    }

    private Player attackerOf(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    /** Protections (spawn, nouveaux joueurs) : avant que les dégâts soient appliqués. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attackerOf(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;

        if (inSpawnZone(victim) || inSpawnZone(attacker)) {
            event.setCancelled(true);
            attacker.sendActionBar(Component.text("🛡 Zone sans PvP (spawn)", NamedTextColor.AQUA));
            return;
        }
        if (isProtected(victim)) {
            event.setCancelled(true);
            attacker.sendActionBar(Component.text("🛡 " + victim.getName() + " est un nouveau joueur protégé.", NamedTextColor.AQUA));
            return;
        }
        if (isProtected(attacker)) {
            protectedUntil.remove(attacker.getUniqueId()); // attaquer fait perdre la protection
            attacker.sendActionBar(Component.text("⚔ Tu as perdu ta protection de nouveau joueur.", NamedTextColor.YELLOW));
        }
    }

    /** Pose le combat tag une fois les dégâts réellement appliqués. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvpTag(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("combat.enabled", true)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attackerOf(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        tag(victim, attacker);
        tag(attacker, victim);
    }

    private void tag(Player player, Player other) {
        boolean already = isTagged(player);
        long seconds = plugin.getConfig().getLong("combat.tag-seconds", 15);
        tagUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        lastAttacker.put(player.getUniqueId(), other.getUniqueId());
        if (!already) {
            player.sendActionBar(Component.text("⚔ En combat ! Ne te déconnecte pas (" + seconds + "s)", NamedTextColor.RED));
        }
    }

    /** Appelé chaque seconde : prévient les joueurs dont le combat tag vient de se terminer. */
    public void tickExpiry() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = tagUntil.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now >= entry.getValue()) {
                it.remove();
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null) {
                    p.sendActionBar(Component.text("✔ Tu n'es plus en combat.", NamedTextColor.GREEN));
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        tagUntil.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int seconds = plugin.getConfig().getInt("combat.newbie-protection-seconds", 300);
        if (!player.hasPlayedBefore() && seconds > 0) {
            protectedUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
            player.sendActionBar(Component.text("🛡 Protection nouveau joueur : " + (seconds / 60) + " min sans PvP", NamedTextColor.AQUA));
            Sfx.click(player);
        }
    }

    /** Déconnexion en plein combat = mort (le dernier adversaire est crédité du kill). */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Long until = tagUntil.remove(id);
        UUID attackerId = lastAttacker.remove(id);
        protectedUntil.remove(id);
        if (until == null || System.currentTimeMillis() >= until || player.isDead()) return;
        if (!plugin.getConfig().getBoolean("combat.enabled", true)) return;
        // un kick (ex. arrêt du serveur) n'est pas un "combat log"
        PlayerQuitEvent.QuitReason reason = event.getReason();
        if (reason == PlayerQuitEvent.QuitReason.KICKED || reason == PlayerQuitEvent.QuitReason.ERRONEOUS_STATE) return;

        Bukkit.broadcast(Component.text("⚠ " + player.getName() + " s'est déconnecté en plein combat !", NamedTextColor.RED));
        Player attacker = attackerId == null ? null : Bukkit.getPlayer(attackerId);
        if (attacker != null && attacker.isOnline() && !attacker.equals(player)) {
            player.damage(0.01, attacker); // enregistre le tueur pour la prime
        }
        player.setHealth(0.0);
    }
}
