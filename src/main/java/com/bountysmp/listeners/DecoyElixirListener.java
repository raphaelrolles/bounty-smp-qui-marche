package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Gère l'"Élixir du Menteur" du Marché Noir : brouille temporairement les
 * signaux du Tracker de tous ceux qui te traquent (coordonnées totalement fausses).
 */
public class DecoyElixirListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey decoyKey;

    public DecoyElixirListener(BountySMP plugin) {
        this.plugin = plugin;
        this.decoyKey = new NamespacedKey(plugin, "bounty_decoy_elixir");
    }

    public NamespacedKey getDecoyKey() {
        return decoyKey;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(decoyKey, PersistentDataType.BYTE);
        if (flag == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.hasCooldown(Material.HONEY_BOTTLE)) {
            player.sendMessage(Component.text("L'Élixir du Menteur recharge encore...", NamedTextColor.GRAY));
            return;
        }

        int durationSeconds = plugin.getConfig().getInt("decoy-elixir.duration-seconds", 60);
        int cooldownSeconds = plugin.getConfig().getInt("item-cooldowns.decoy-elixir-seconds", 45);
        player.setCooldown(Material.HONEY_BOTTLE, cooldownSeconds * 20);

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        data.setDecoyUntil(System.currentTimeMillis() + durationSeconds * 1000L);

        player.getWorld().spawnParticle(org.bukkit.Particle.WITCH, player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITCH_DRINK, 1f, 1.2f);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        player.sendMessage(Component.text("🐍 Tes traqueurs vont recevoir de fausses coordonnées pendant " + durationSeconds + "s...", NamedTextColor.DARK_PURPLE));
    }
}
