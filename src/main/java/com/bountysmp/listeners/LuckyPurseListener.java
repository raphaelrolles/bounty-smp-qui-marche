package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Gère la "Bourse Chanceuse" du Marché Noir : à usage unique, ouverture pariée
 * qui rapporte un montant de coins aléatoire (parfois rien, parfois le jackpot).
 */
public class LuckyPurseListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey purseKey;

    public LuckyPurseListener(BountySMP plugin) {
        this.plugin = plugin;
        this.purseKey = new NamespacedKey(plugin, "bounty_lucky_purse");
    }

    public NamespacedKey getPurseKey() {
        return purseKey;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(purseKey, PersistentDataType.BYTE);
        if (flag == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        int min = plugin.getConfig().getInt("lucky-purse.min-reward", 0);
        int jackpot = plugin.getConfig().getInt("lucky-purse.jackpot-reward", 300);

        int roll = ThreadLocalRandom.current().nextInt(100);
        int reward;
        String message;
        if (roll < 10) {
            reward = jackpot;
            message = "🎉 JACKPOT ! Tu trouves ";
        } else if (roll < 40) {
            reward = min;
            message = "😕 La bourse est vide, tu ne gagnes rien.";
        } else {
            reward = ThreadLocalRandom.current().nextInt(20, 80);
            message = "💰 Tu trouves ";
        }

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (reward > 0) {
            data.addCoins(reward);
            player.sendMessage(Component.text(message, NamedTextColor.GOLD)
                    .append(Component.text(reward + " coins", NamedTextColor.YELLOW))
                    .append(Component.text(" dans la bourse !", NamedTextColor.GOLD)));
        } else {
            player.sendMessage(Component.text(message, NamedTextColor.GRAY));
        }

        player.getWorld().spawnParticle(org.bukkit.Particle.WAX_ON, player.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.02);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }
    }
}
