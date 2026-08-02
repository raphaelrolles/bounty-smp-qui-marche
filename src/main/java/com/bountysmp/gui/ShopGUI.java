package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Boutique Bounty : /bounty shop
 * Page 0 = Kits (Hunter Kit, Diamond Kit, Netherite Kit)
 * Page 1 = Objets (Tracker, Fumigène du Fugitif, Vendre des minerais)
 */
public class ShopGUI implements Listener {

    private static final int TOTAL_PAGES = 2;

    /** Marqueur d'identité fiable pour l'inventaire boutique, contient la page affichée. */
    private static class ShopHolder implements InventoryHolder {
        final int page;
        ShopHolder(int page) { this.page = page; }
        @Override
        public Inventory getInventory() { return null; }
    }

    private final BountySMP plugin;
    private final NamespacedKey shopItemKey;
    private final NamespacedKey trackerKey;
    private final NamespacedKey smokeBombKey;

    public ShopGUI(BountySMP plugin) {
        this.plugin = plugin;
        this.shopItemKey = new NamespacedKey(plugin, "bounty_shop_item");
        this.trackerKey = new NamespacedKey(plugin, "bounty_tracker");
        this.smokeBombKey = new NamespacedKey(plugin, "bounty_smoke_bomb");
    }

    public NamespacedKey getSmokeBombKey() {
        return smokeBombKey;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        String title = page == 0 ? "Boutique - Kits" : "Boutique - Objets";
        Inventory inv = Bukkit.createInventory(new ShopHolder(page), 27, Component.text(title, NamedTextColor.DARK_RED));

        if (page == 0) {
            populateKitsPage(inv, data);
        } else {
            populateItemsPage(inv, data);
        }

        // Solde du joueur
        ItemStack balanceItem = new ItemStack(Material.SUNFLOWER);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        balanceMeta.displayName(Component.text("§6Ton solde").decoration(TextDecoration.ITALIC, false));
        balanceMeta.lore(List.of(Component.text("§e" + (long) data.getCoins() + " Bounty Coins").decoration(TextDecoration.ITALIC, false)));
        balanceItem.setItemMeta(balanceMeta);
        inv.setItem(4, balanceItem);

        // Navigation
        if (page > 0) {
            inv.setItem(18, buildNavItem(Material.ARROW, "§7« Page précédente", "nav_prev"));
        }
        if (page < TOTAL_PAGES - 1) {
            inv.setItem(26, buildNavItem(Material.ARROW, "§7Page suivante »", "nav_next"));
        }

        player.openInventory(inv);
    }

    private void populateKitsPage(Inventory inv, PlayerData data) {
        int hunterPrice = plugin.getConfig().getInt("shop.hunter-kit-price", 150);
        int diamondPrice = plugin.getConfig().getInt("shop.diamond-kit-price", 400);
        int netheritePrice = plugin.getConfig().getInt("shop.netherite-kit-price", 1500);

        inv.setItem(10, buildShopItem(Material.GOLDEN_APPLE, "hunter_kit",
                "§aHunter Kit", hunterPrice,
                List.of("§7Pommes dorées, nourriture,", "§7flèches et un arc.", "", "§ePrix : " + hunterPrice + " coins")));

        boolean diamondOwned = data.hasPurchased("diamond_kit");
        inv.setItem(13, buildShopItem(Material.DIAMOND_CHESTPLATE, "diamond_kit",
                diamondOwned ? "§8Kit Diamant (déjà acheté)" : "§bKit Diamant", diamondPrice,
                diamondOwned
                        ? List.of("§8Achat unique déjà utilisé.")
                        : List.of("§7Armure diamant complète,", "§7épée et pioche en diamant.",
                        "§c⚠ Achat unique !", "", "§ePrix : " + diamondPrice + " coins")));

        boolean netheriteOwned = data.hasPurchased("netherite_kit");
        inv.setItem(16, buildShopItem(Material.NETHERITE_CHESTPLATE, "netherite_kit",
                netheriteOwned ? "§8Kit Netherite (déjà acheté)" : "§4Kit Netherite", netheritePrice,
                netheriteOwned
                        ? List.of("§8Achat unique déjà utilisé.")
                        : List.of("§7Armure netherite complète,", "§7épée, hache et pommes dorées enchantées.",
                        "§c⚠ Achat unique ! Très cher.", "", "§ePrix : " + netheritePrice + " coins")));
    }

    private void populateItemsPage(Inventory inv, PlayerData data) {
        int trackerPrice = plugin.getConfig().getInt("shop.tracker-price", 50);
        int smokePrice = plugin.getConfig().getInt("shop.smoke-bomb-price", 80);

        inv.setItem(10, buildShopItem(Material.COMPASS, "tracker",
                "§6Tracker", trackerPrice,
                List.of("§7Clic droit + Maj : choisir une cible", "§7Clic droit : coordonnées approximatives",
                        "§7(imprécises, avec cooldown).", "", "§ePrix : " + trackerPrice + " coins")));

        inv.setItem(13, buildShopItem(Material.GUNPOWDER, "smoke_bomb",
                "§7Fumigène du Fugitif", smokePrice,
                List.of("§7Clic droit : invisibilité et vitesse", "§7temporaires pour semer tes chasseurs.",
                        "", "§ePrix : " + smokePrice + " coins")));

        int diamondRate = plugin.getConfig().getInt("ore-exchange.diamond-rate", 5);
        int emeraldRate = plugin.getConfig().getInt("ore-exchange.emerald-rate", 2);
        int netheriteRate = plugin.getConfig().getInt("ore-exchange.netherite-ingot-rate", 30);
        ItemStack sellItem = buildShopItem(Material.HOPPER, "sell_ores", "§eVendre des minerais", 0,
                List.of("§7Échange tous tes diamants,", "§7émeraudes et lingots de netherite", "§7présents dans ton inventaire.",
                        "", "§71 diamant = " + diamondRate + " coins",
                        "§71 émeraude = " + emeraldRate + " coins",
                        "§71 lingot de netherite = " + netheriteRate + " coins"));
        inv.setItem(16, sellItem);
    }

    private ItemStack buildShopItem(Material material, String id, String name, int price, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(l -> Component.text(l).decoration(TextDecoration.ITALIC, false)).toList());
        meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavItem(Material material, String name, String id) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof ShopHolder)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;

        String id = clicked.getItemMeta().getPersistentDataContainer().get(shopItemKey, PersistentDataType.STRING);
        if (id == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (id.equals("nav_prev")) {
            open(player, holder.page - 1);
            return;
        }
        if (id.equals("nav_next")) {
            open(player, holder.page + 1);
            return;
        }

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        switch (id) {
            case "hunter_kit" -> purchase(player, data, "shop.hunter-kit-price", 150, null, this::giveHunterKit);
            case "diamond_kit" -> purchase(player, data, "shop.diamond-kit-price", 400, "diamond_kit", this::giveDiamondKit);
            case "netherite_kit" -> purchase(player, data, "shop.netherite-kit-price", 1500, "netherite_kit", this::giveNetheriteKit);
            case "tracker" -> purchase(player, data, "shop.tracker-price", 50, null, this::giveTracker);
            case "smoke_bomb" -> purchase(player, data, "shop.smoke-bomb-price", 80, null, this::giveSmokeBomb);
            case "sell_ores" -> sellOres(player, data);
        }

        // Rafraîchit l'affichage au tick suivant (solde à jour, kit peut-être marqué "déjà acheté")
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> open(player, holder.page));
    }

    private interface Rewarder {
        void give(Player player);
    }

    /**
     * @param oneTimeId si non-null, l'objet ne peut être acheté qu'une seule fois par joueur.
     */
    private void purchase(Player player, PlayerData data, String configPath, int defaultPrice, String oneTimeId, Rewarder rewarder) {
        if (oneTimeId != null && data.hasPurchased(oneTimeId)) {
            player.sendMessage(Component.text("Tu as déjà acheté cet objet, il n'est disponible qu'une seule fois.", NamedTextColor.RED));
            return;
        }
        int price = plugin.getConfig().getInt(configPath, defaultPrice);
        if (data.getCoins() < price) {
            player.sendMessage(Component.text("Tu n'as pas assez de Bounty Coins (" + (long) data.getCoins() + "/" + price + ").", NamedTextColor.RED));
            return;
        }
        data.addCoins(-price);
        if (oneTimeId != null) {
            data.getOneTimePurchases().add(oneTimeId);
        }
        rewarder.give(player);
        player.sendMessage(Component.text("Achat effectué pour " + price + " coins !", NamedTextColor.GREEN));
    }

    private void sellOres(Player player, PlayerData data) {
        int diamondRate = plugin.getConfig().getInt("ore-exchange.diamond-rate", 5);
        int emeraldRate = plugin.getConfig().getInt("ore-exchange.emerald-rate", 2);
        int netheriteRate = plugin.getConfig().getInt("ore-exchange.netherite-ingot-rate", 30);

        int diamonds = countAndRemove(player, Material.DIAMOND);
        int emeralds = countAndRemove(player, Material.EMERALD);
        int netherite = countAndRemove(player, Material.NETHERITE_INGOT);

        double total = diamonds * diamondRate + emeralds * emeraldRate + netherite * netheriteRate;

        if (total <= 0) {
            player.sendMessage(Component.text("Tu n'as aucun diamant, émeraude ou lingot de netherite à vendre.", NamedTextColor.RED));
            return;
        }

        data.addCoins(total);
        player.sendMessage(Component.text("Vendu : ", NamedTextColor.GREEN)
                .append(Component.text(diamonds + " diamants, " + emeralds + " émeraudes, " + netherite + " lingots de netherite", NamedTextColor.AQUA))
                .append(Component.text(" contre " + (long) total + " coins !", NamedTextColor.GREEN)));
    }

    private int countAndRemove(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        if (count > 0) {
            player.getInventory().remove(material);
        }
        return count;
    }

    private void giveTracker(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.displayName(Component.text("§6Tracker de Prime").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit + Maj : choisir une cible").decoration(TextDecoration.ITALIC, false),
                Component.text("§7Clic droit : coordonnées approximatives.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(trackerKey, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        player.getInventory().addItem(compass);
    }

    private void giveSmokeBomb(Player player) {
        ItemStack item = new ItemStack(Material.GUNPOWDER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§7Fumigène du Fugitif").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : invisibilité et vitesse").decoration(TextDecoration.ITALIC, false),
                Component.text("§7pendant 10 secondes.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(smokeBombKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }

    private void giveHunterKit(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.GOLDEN_APPLE, 2),
                new ItemStack(Material.BREAD, 16),
                new ItemStack(Material.ARROW, 32),
                new ItemStack(Material.BOW, 1)
        );
    }

    private void giveDiamondKit(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.DIAMOND_HELMET),
                new ItemStack(Material.DIAMOND_CHESTPLATE),
                new ItemStack(Material.DIAMOND_LEGGINGS),
                new ItemStack(Material.DIAMOND_BOOTS),
                new ItemStack(Material.DIAMOND_SWORD),
                new ItemStack(Material.DIAMOND_PICKAXE)
        );
    }

    private void giveNetheriteKit(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.NETHERITE_HELMET),
                new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.NETHERITE_LEGGINGS),
                new ItemStack(Material.NETHERITE_BOOTS),
                new ItemStack(Material.NETHERITE_SWORD),
                new ItemStack(Material.NETHERITE_AXE),
                new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2)
        );
    }
}
