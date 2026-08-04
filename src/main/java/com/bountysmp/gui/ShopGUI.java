package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Boutique Bounty : /bounty shop
 * Page 0 = Kits (Kit Fer, Kit Diamant, Kit Netherite [unique])
 * Page 1 = Objets (Tracker, Feu d'artifice du Fugitif, Lame du Bourreau, Vendre des minerais)
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
        int ironPrice = plugin.getConfig().getInt("shop.iron-kit-price", 120);
        int diamondPrice = plugin.getConfig().getInt("shop.diamond-kit-price", 400);
        int netheritePrice = plugin.getConfig().getInt("shop.netherite-kit-price", 1500);

        inv.setItem(10, buildShopItem(Material.IRON_CHESTPLATE, "iron_kit",
                "§fKit Fer", ironPrice,
                List.of("§7Armure fer complète,", "§7épée et pioche en fer.", "", "§ePrix : " + ironPrice + " coins")));

        inv.setItem(13, buildShopItem(Material.DIAMOND_CHESTPLATE, "diamond_kit",
                "§bKit Diamant", diamondPrice,
                List.of("§7Armure diamant complète,", "§7épée et pioche en diamant.", "", "§ePrix : " + diamondPrice + " coins")));

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
        int bladePrice = plugin.getConfig().getInt("shop.executioner-blade-price", 2500);

        inv.setItem(10, buildShopItem(Material.COMPASS, "tracker",
                "§6Tracker", trackerPrice,
                List.of("§7Clic droit + Maj : choisir une cible", "§7Clic droit : coordonnées approximatives",
                        "§7(imprécises, avec cooldown).", "", "§ePrix : " + trackerPrice + " coins")));

        inv.setItem(12, buildShopItem(Material.FIREWORK_ROCKET, "smoke_bomb",
                "§bFeu d'Artifice du Fugitif", smokePrice,
                List.of("§7Clic droit : invisibilité et vitesse", "§7temporaires pour semer tes chasseurs.",
                        "", "§ePrix : " + smokePrice + " coins")));

        inv.setItem(14, buildShopItem(Material.NETHERITE_SWORD, "executioner_blade",
                "§4§lLame du Bourreau", bladePrice,
                List.of("§7Une lame surenchantée pour", "§7les chasseurs les plus riches.",
                        "§7Tranchant V, Butin III,", "§7Aspect du Feu II, Solidité III.",
                        "", "§ePrix : " + bladePrice + " coins")));

        int diamondRate = plugin.getConfig().getInt("ore-exchange.diamond-rate", 5);
        int emeraldRate = plugin.getConfig().getInt("ore-exchange.emerald-rate", 2);
        int netheriteRate = plugin.getConfig().getInt("ore-exchange.netherite-ingot-rate", 30);
        int dailyLimit = plugin.getConfig().getInt("ore-exchange.daily-limit", 3);
        int usedToday = currentEpochDay() == data.getOreExchangeEpochDay() ? data.getOreExchangesToday() : 0;

        ItemStack sellItem = buildShopItem(Material.HOPPER, "sell_ores", "§eVendre des minerais", 0,
                List.of("§7Échange tous tes diamants,", "§7émeraudes et lingots de netherite", "§7présents dans ton inventaire.",
                        "", "§71 diamant = " + diamondRate + " coins",
                        "§71 émeraude = " + emeraldRate + " coins",
                        "§71 lingot de netherite = " + netheriteRate + " coins",
                        "", "§7Utilisations aujourd'hui : §f" + usedToday + "/" + dailyLimit));
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
        int purchaseSlot = event.getSlot();

        switch (id) {
            case "iron_kit" -> purchase(player, data, "shop.iron-kit-price", 120, null, this::giveIronKit, purchaseSlot);
            case "diamond_kit" -> purchase(player, data, "shop.diamond-kit-price", 400, null, this::giveDiamondKit, purchaseSlot);
            case "netherite_kit" -> purchase(player, data, "shop.netherite-kit-price", 1500, "netherite_kit", this::giveNetheriteKit, purchaseSlot);
            case "tracker" -> purchase(player, data, "shop.tracker-price", 50, null, this::giveTracker, purchaseSlot);
            case "smoke_bomb" -> purchase(player, data, "shop.smoke-bomb-price", 80, null, this::giveSmokeBomb, purchaseSlot);
            case "executioner_blade" -> purchase(player, data, "shop.executioner-blade-price", 2500, null, this::giveExecutionerBlade, purchaseSlot);
            case "sell_ores" -> sellOres(player, data);
        }

        // Rafraîchit l'affichage au tick suivant (solde à jour, kit peut-être marqué "déjà acheté")
        Bukkit.getScheduler().runTask(plugin, () -> open(player, holder.page));
    }

    private interface Rewarder {
        void give(Player player);
    }

    /**
     * @param oneTimeId si non-null, l'objet ne peut être acheté qu'une seule fois par joueur.
     */
    private void purchase(Player player, PlayerData data, String configPath, int defaultPrice, String oneTimeId, Rewarder rewarder, int slot) {
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
        playPurchaseAnimation(player, slot);
    }

    /** Petite animation/feedback visuel + sonore à l'achat. */
    private void playPurchaseAnimation(Player player, int slot) {
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getEyeLocation(), 12, 0.3, 0.3, 0.3, 0);
    }

    private long currentEpochDay() {
        return Instant.now().atZone(ZoneOffset.UTC).toLocalDate().toEpochDay();
    }

    private void sellOres(Player player, PlayerData data) {
        int dailyLimit = plugin.getConfig().getInt("ore-exchange.daily-limit", 3);
        long today = currentEpochDay();

        if (data.getOreExchangeEpochDay() != today) {
            data.setOreExchangeEpochDay(today);
            data.setOreExchangesToday(0);
        }

        if (data.getOreExchangesToday() >= dailyLimit) {
            player.sendMessage(Component.text("Tu as atteint la limite quotidienne d'échanges de minerais (" + dailyLimit + "/jour). Reviens demain !", NamedTextColor.RED));
            return;
        }

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
        data.setOreExchangesToday(data.getOreExchangesToday() + 1);
        player.sendMessage(Component.text("Vendu : ", NamedTextColor.GREEN)
                .append(Component.text(diamonds + " diamants, " + emeralds + " émeraudes, " + netherite + " lingots de netherite", NamedTextColor.AQUA))
                .append(Component.text(" contre " + (long) total + " coins !", NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Échanges restants aujourd'hui : " + (dailyLimit - data.getOreExchangesToday()), NamedTextColor.GRAY));
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

    /**
     * Donne des objets au joueur ; ceux qui ne tiennent pas dans l'inventaire
     * tombent au sol à ses pieds au lieu d'être perdus.
     */
    private void giveItems(Player player, ItemStack... items) {
        var leftovers = player.getInventory().addItem(items);
        if (!leftovers.isEmpty()) {
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItem(player.getLocation(), leftover);
            }
            player.sendMessage(Component.text("⚠ Ton inventaire était plein, certains objets sont tombés au sol.", NamedTextColor.YELLOW));
        }
    }

    private void giveTracker(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.displayName(Component.text("§6Tracker de Prime").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit + Maj : choisir une cible").decoration(TextDecoration.ITALIC, false),
                Component.text("§7Clic droit : coordonnées approximatives.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(trackerKey, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        giveItems(player, compass);
    }

    private void giveSmokeBomb(Player player) {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§bFeu d'Artifice du Fugitif").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : invisibilité et vitesse").decoration(TextDecoration.ITALIC, false),
                Component.text("§7pendant 10 secondes, aveugle les joueurs").decoration(TextDecoration.ITALIC, false),
                Component.text("§7proches pendant 2 secondes.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(smokeBombKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        giveItems(player, item);
    }

    private void giveExecutionerBlade(Player player) {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.displayName(Component.text("§4§lLame du Bourreau").decoration(TextDecoration.ITALIC, false));
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.LOOTING, 3, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        sword.setItemMeta(meta);
        giveItems(player, sword);
    }

    private void giveIronKit(Player player) {
        giveItems(player,
                new ItemStack(Material.IRON_HELMET),
                new ItemStack(Material.IRON_CHESTPLATE),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.IRON_SWORD),
                new ItemStack(Material.IRON_PICKAXE)
        );
    }

    private void giveDiamondKit(Player player) {
        giveItems(player,
                new ItemStack(Material.DIAMOND_HELMET),
                new ItemStack(Material.DIAMOND_CHESTPLATE),
                new ItemStack(Material.DIAMOND_LEGGINGS),
                new ItemStack(Material.DIAMOND_BOOTS),
                new ItemStack(Material.DIAMOND_SWORD),
                new ItemStack(Material.DIAMOND_PICKAXE)
        );
    }

    private void giveNetheriteKit(Player player) {
        giveItems(player,
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
