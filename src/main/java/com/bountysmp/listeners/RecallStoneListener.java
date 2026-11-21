package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.util.ItemUse;
import com.bountysmp.util.Sfx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pierre de Rappel : clic droit, puis le joueur doit rester immobile quelques secondes.
 * Si rien ne l'interrompt (mouvement, dégâts), il est téléporté à son lit ou au spawn du monde,
 * et la pierre est consommée.
 */
public class RecallStoneListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey stoneKey;
    private final Map<UUID, BukkitRunnable> channels = new HashMap<>();

    public RecallStoneListener(BountySMP plugin) {
        this.plugin = plugin;
        this.stoneKey = new NamespacedKey(plugin, "bounty_recall_stone");
    }

    private boolean isStone(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().get(stoneKey, PersistentDataType.BYTE) != null;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isStone(event.getItem())) return;
        if (ItemUse.guardOffhand(plugin, event)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (channels.containsKey(player.getUniqueId())) {
            return; // rappel déjà en cours (l'événement peut arriver deux fois, une par main)
        }
        if (player.hasCooldown(Material.AMETHYST_SHARD)) {
            player.sendActionBar(Component.text("La Pierre de Rappel recharge encore...", NamedTextColor.GRAY));
            Sfx.cooldown(player);
            return;
        }
        if (plugin.getCombatListener() != null && plugin.getCombatListener().isTagged(player)) {
            player.sendActionBar(Component.text("⚔ Impossible en combat !", NamedTextColor.RED));
            Sfx.denied(player);
            return;
        }
        startChannel(player);
    }

    private void startChannel(Player player) {
        final int seconds = Math.max(2, plugin.getConfig().getInt("recall-stone.channel-seconds", 8));
        final Location start = player.getLocation().clone();
        final UUID id = player.getUniqueId();

        Sfx.recallStart(player);
        player.sendActionBar(Component.text("🧭 Rappel dans " + seconds + "s... ne bouge pas !", NamedTextColor.AQUA));

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    channels.remove(id);
                    cancel();
                    return;
                }
                Location now = player.getLocation();
                double dx = now.getX() - start.getX();
                double dz = now.getZ() - start.getZ();
                if (now.getWorld() != start.getWorld() || dx * dx + dz * dz > 0.5) {
                    abort(player, "Tu as bougé, rappel annulé.");
                    return;
                }
                ticks += 5;
                player.getWorld().spawnParticle(Particle.PORTAL, now.clone().add(0, 1, 0), 6, 0.4, 0.8, 0.4, 0.2);
                if (ticks % 20 == 0) {
                    int remaining = seconds - ticks / 20;
                    if (remaining <= 0) {
                        finish(player);
                        return;
                    }
                    player.sendActionBar(Component.text("🧭 Rappel dans " + remaining + "s... ne bouge pas !", NamedTextColor.AQUA));
                    Sfx.recallTick(player, remaining);
                }
            }
        };
        channels.put(id, task);
        task.runTaskTimer(plugin, 5L, 5L);
    }

    private void abort(Player player, String message) {
        BukkitRunnable task = channels.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        player.setCooldown(Material.AMETHYST_SHARD, 60);
        player.sendActionBar(Component.text(message, NamedTextColor.RED));
        Sfx.recallCancel(player);
    }

    private void finish(Player player) {
        BukkitRunnable task = channels.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        if (!consumeStone(player)) {
            abort(player, "Tu n'as plus ta Pierre de Rappel.");
            return;
        }
        Location from = player.getLocation().clone();
        Location dest = player.getBedSpawnLocation();
        if (dest == null) {
            dest = Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        player.setFallDistance(0f);
        player.teleport(dest);

        int cooldownSeconds = plugin.getConfig().getInt("item-cooldowns.recall-stone-seconds", 20);
        player.setCooldown(Material.AMETHYST_SHARD, cooldownSeconds * 20);

        from.getWorld().spawnParticle(Particle.REVERSE_PORTAL, from.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.1);
        dest.getWorld().spawnParticle(Particle.REVERSE_PORTAL, dest.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.1);
        Sfx.recallDone(from, dest);
        player.sendActionBar(Component.text("🧭 Rappel effectué !", NamedTextColor.AQUA));
    }

    private boolean consumeStone(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isStone(s)) {
                if (s.getAmount() > 1) {
                    s.setAmount(s.getAmount() - 1);
                } else {
                    inv.setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && channels.containsKey(player.getUniqueId())
                && event.getFinalDamage() > 0) {
            abort(player, "Tu as été touché, rappel annulé.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BukkitRunnable task = channels.remove(event.getPlayer().getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
}
