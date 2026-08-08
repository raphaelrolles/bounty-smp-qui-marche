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

import java.util.Comparator;
import java.util.Optional;

/**
 * Gère la "Bourse Trouée" du Marché Noir : à usage unique, vole des coins
 * au joueur en ligne le plus proche.
 */
public class PickpocketListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey pickpocketKey;

    public PickpocketListener(BountySMP plugin) {
        this.plugin = plugin;
        this.pickpocketKey = new NamespacedKey(plugin, "bounty_pickpocket");
    }

    public NamespacedKey getPickpocketKey() {
        return pickpocketKey;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(pickpocketKey, PersistentDataType.BYTE);
        if (flag == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        double maxRange = plugin.getConfig().getDouble("pickpocket.max-range", 10.0);
        Optional<Player> nearest = player.getWorld().getPlayers().stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .filter(p -> p.getLocation().distance(player.getLocation()) <= maxRange)
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(player.getLocation())));

        if (nearest.isEmpty()) {
            player.sendMessage(Component.text("Personne d'assez proche pour être délesté (" + (int) maxRange + " blocs max).", NamedTextColor.GRAY));
            return;
        }

        Player target = nearest.get();
        int amount = plugin.getConfig().getInt("pickpocket.steal-amount", 15);

        PlayerData targetData = plugin.getDataManager().get(target.getUniqueId());
        PlayerData playerData = plugin.getDataManager().get(player.getUniqueId());

        int stolen = (int) Math.min(amount, targetData.getCoins());

        // Consommation de l'objet dans tous les cas (usage unique)
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        if (stolen <= 0) {
            player.sendMessage(Component.text("La bourse de " + target.getName() + " est vide... Coup pour rien.", NamedTextColor.GRAY));
            return;
        }

        targetData.addCoins(-stolen);
        playerData.addCoins(stolen);

        player.sendMessage(Component.text("🤏 Tu as volé ", NamedTextColor.YELLOW)
                .append(Component.text(stolen + " coins", NamedTextColor.GOLD))
                .append(Component.text(" à " + target.getName() + " !", NamedTextColor.YELLOW)));
        target.sendMessage(Component.text("🤏 On vient de te voler ", NamedTextColor.RED)
                .append(Component.text(stolen + " coins", NamedTextColor.GOLD))
                .append(Component.text(" !", NamedTextColor.RED)));
    }
}
