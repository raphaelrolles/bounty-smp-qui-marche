package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Menu spécial pour /sell : le joueur dépose les minerais qu'il veut vendre
 * dans un inventaire dédié. Fermer l'inventaire (Échap) valide la vente et
 * convertit le contenu en Bounty Coins. Les objets non reconnus sont rendus.
 */
public class SellGUI implements Listener {

    private static class SellHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    private final BountySMP plugin;

    public SellGUI(BountySMP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new SellHolder(), 27,
                Component.text("Vendre des minerais - Ferme pour valider", NamedTextColor.GOLD));
        player.openInventory(inv);
        player.sendMessage(Component.text("Dépose tes minerais ici, puis ferme l'inventaire (touche Échap) pour toucher tes coins.", NamedTextColor.YELLOW));
    }

    private long currentEpochDay() {
        return Instant.now().atZone(ZoneOffset.UTC).toLocalDate().toEpochDay();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellHolder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        int dailyLimit = plugin.getConfig().getInt("ore-exchange.daily-limit", 8);
        long today = currentEpochDay();
        if (data.getOreExchangeEpochDay() != today) {
            data.setOreExchangeEpochDay(today);
            data.setOreExchangesToday(0);
        }
        if (data.getOreExchangesToday() >= dailyLimit) {
            // Rend tout le contenu, la limite quotidienne est atteinte
            returnAllContents(player, event.getInventory());
            player.sendMessage(Component.text("Tu as atteint la limite quotidienne de ventes (" + dailyLimit + "/jour). Objets rendus.", NamedTextColor.RED));
            return;
        }

        Map<Material, Double> rates = new LinkedHashMap<>();
        rates.put(Material.DIAMOND, plugin.getConfig().getDouble("ore-exchange.diamond-rate", 8));
        rates.put(Material.EMERALD, plugin.getConfig().getDouble("ore-exchange.emerald-rate", 4));
        rates.put(Material.NETHERITE_INGOT, plugin.getConfig().getDouble("ore-exchange.netherite-ingot-rate", 50));
        rates.put(Material.GOLD_INGOT, plugin.getConfig().getDouble("ore-exchange.gold-ingot-rate", 2));
        rates.put(Material.IRON_INGOT, plugin.getConfig().getDouble("ore-exchange.iron-ingot-rate", 1));
        rates.put(Material.RAW_GOLD, plugin.getConfig().getDouble("ore-exchange.gold-ingot-rate", 2));
        rates.put(Material.RAW_IRON, plugin.getConfig().getDouble("ore-exchange.iron-ingot-rate", 1));

        double total = 0;
        Map<Material, Integer> sold = new LinkedHashMap<>();
        ItemStack[] contents = event.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;
            Double rate = rates.get(stack.getType());
            if (rate == null) {
                // Objet non reconnu : on le rend au joueur au lieu de le perdre
                giveBack(player, stack);
                continue;
            }
            total += rate * stack.getAmount();
            sold.merge(stack.getType(), stack.getAmount(), Integer::sum);
        }

        if (total <= 0) {
            return; // rien à vendre, rien à annoncer
        }

        data.addCoins(total);
        data.setOreExchangesToday(data.getOreExchangesToday() + 1);

        StringBuilder detail = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : sold.entrySet()) {
            if (detail.length() > 0) detail.append(", ");
            detail.append(entry.getValue()).append(" ").append(prettyName(entry.getKey()));
        }

        player.sendMessage(Component.text("💰 Vendu : ", NamedTextColor.GREEN)
                .append(Component.text(detail.toString(), NamedTextColor.AQUA))
                .append(Component.text(" contre " + (long) total + " coins !", NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Ventes restantes aujourd'hui : " + (dailyLimit - data.getOreExchangesToday()), NamedTextColor.GRAY));
    }

    private void returnAllContents(Player player, Inventory inv) {
        for (ItemStack stack : inv.getContents()) {
            if (stack != null) {
                giveBack(player, stack);
            }
        }
    }

    private void giveBack(Player player, ItemStack stack) {
        var leftovers = player.getInventory().addItem(stack);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    private String prettyName(Material material) {
        return switch (material) {
            case DIAMOND -> "diamants";
            case EMERALD -> "émeraudes";
            case NETHERITE_INGOT -> "lingots de netherite";
            case GOLD_INGOT -> "lingots d'or";
            case IRON_INGOT -> "lingots de fer";
            case RAW_GOLD -> "or brut";
            case RAW_IRON -> "fer brut";
            default -> material.name().toLowerCase();
        };
    }
}
