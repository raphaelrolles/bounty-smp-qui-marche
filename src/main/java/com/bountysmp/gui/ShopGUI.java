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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Boutique Bounty : /bounty shop
 * Page 0 = Kits (Kit Fer, Kit Diamant, Kit Netherite)
 * Page 1 = Objets (Tracker, Feu d'artifice, Fiole de Rage, Grenade du Chaos, Élan du Chasseur, Vendre des minerais)
 * Page 2 = Marché Noir (Faux Lingot d'Or, Élixir du Menteur)
 */
public class ShopGUI implements Listener {

    private static final int TOTAL_PAGES = 3;

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

    public NamespacedKey getTrackerKey() {
        return trackerKey;
    }

    /** Vrai si le joueur possède au moins un Tracker dans son inventaire. */
    public boolean hasTracker(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getItemMeta() == null) continue;
            Byte flag = stack.getItemMeta().getPersistentDataContainer().get(trackerKey, PersistentDataType.BYTE);
            if (flag != null) return true;
        }
        return false;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        String title = switch (page) {
            case 0 -> "Boutique - Kits";
            case 1 -> "Boutique - Objets";
            default -> "Boutique - Marché Noir";
        };
        Inventory inv = Bukkit.createInventory(new ShopHolder(page), 27, Component.text(title, NamedTextColor.DARK_RED));

        if (page == 0) {
            populateKitsPage(inv, data);
        } else if (page == 1) {
            populateItemsPage(inv, data);
        } else {
            populateBlackMarketPage(inv, data);
        }

        // Solde du joueur
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
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
        inv.setItem(22, buildNavItem(Material.BARRIER, "§7« Retour au menu", "back_menu"));

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

        inv.setItem(16, buildShopItem(Material.NETHERITE_CHESTPLATE, "netherite_kit",
                "§4Kit Netherite", netheritePrice,
                List.of("§7Armure netherite complète,", "§7épée, hache et pommes dorées enchantées.",
                        "", "§ePrix : " + netheritePrice + " coins")));
    }

    private void populateItemsPage(Inventory inv, PlayerData data) {
        int trackerPrice = plugin.getConfig().getInt("shop.tracker-price", 50);
        int smokePrice = plugin.getConfig().getInt("shop.smoke-bomb-price", 80);
        int chaosPrice = plugin.getConfig().getInt("shop.chaos-grenade-price", 120);
        int dashPrice = plugin.getConfig().getInt("shop.dash-item-price", 100);
        int ragePrice = plugin.getConfig().getInt("shop.rage-vial-price", 90);

        inv.setItem(10, buildShopItem(Material.COMPASS, "tracker",
                "§6Tracker", trackerPrice,
                List.of("§7Clic droit + Maj : choisir une cible", "§7Clic droit : coordonnées approximatives",
                        "§7(imprécises, cooldown façon perle d'ender).", "", "§ePrix : " + trackerPrice + " coins")));

        inv.setItem(11, buildShopItem(Material.FIREWORK_ROCKET, "smoke_bomb",
                "§bFeu d'Artifice du Fugitif", smokePrice,
                List.of("§7Clic droit : invisibilité et vitesse", "§710s, aveugle les joueurs proches 2s.",
                        "", "§ePrix : " + smokePrice + " coins")));

        inv.setItem(12, buildShopItem(Material.POTION, "rage_vial",
                "§6Fiole de Rage", ragePrice,
                List.of("§7Clic droit : Force et Résistance au feu", "§7pendant 15 secondes.",
                        "", "§ePrix : " + ragePrice + " coins")));

        inv.setItem(13, buildShopItem(Material.SNOWBALL, "chaos_grenade",
                "§dGrenade du Chaos", chaosPrice,
                List.of("§7Clic droit : lance une grenade qui", "§7applique un effet négatif aléatoire",
                        "§7au joueur touché.", "", "§ePrix : " + chaosPrice + " coins")));

        inv.setItem(15, buildShopItem(Material.FEATHER, "dash_item",
                "§bÉlan du Chasseur", dashPrice,
                List.of("§7Clic droit : propulsion rapide", "§7en avant, sans dégâts de chute.",
                        "", "§ePrix : " + dashPrice + " coins")));

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

    private void populateBlackMarketPage(Inventory inv, PlayerData data) {
        int ingotPrice = plugin.getConfig().getInt("shop.fake-ingot-price", 60);
        int elixirPrice = plugin.getConfig().getInt("shop.decoy-elixir-price", 200);
        int elixirDuration = plugin.getConfig().getInt("decoy-elixir.duration-seconds", 60);

        inv.setItem(11, buildShopItem(Material.GOLD_NUGGET, "fake_ingot",
                "§6Faux Lingot d'Or", ingotPrice,
                List.of("§7Laisse-le traîner au sol :", "§7quiconque le ramasse (sauf toi)",
                        "§7est maudit (lenteur + faiblesse).", "", "§ePrix : " + ingotPrice + " coins")));

        inv.setItem(15, buildShopItem(Material.HONEY_BOTTLE, "decoy_elixir",
                "§5Élixir du Menteur", elixirPrice,
                List.of("§7Clic droit : brouille pendant " + elixirDuration + "s", "§7les signaux de tout Tracker",
                        "§7pointé sur toi.", "", "§ePrix : " + elixirPrice + " coins")));
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
        if (id.equals("back_menu")) {
            plugin.getBountyMenuGUI().open(player);
            return;
        }

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        int purchaseSlot = event.getSlot();

        switch (id) {
            case "iron_kit" -> purchase(player, data, "shop.iron-kit-price", 120, this::giveIronKit, purchaseSlot);
            case "diamond_kit" -> purchase(player, data, "shop.diamond-kit-price", 400, this::giveDiamondKit, purchaseSlot);
            case "netherite_kit" -> purchase(player, data, "shop.netherite-kit-price", 1500, this::giveNetheriteKit, purchaseSlot);
            case "tracker" -> purchase(player, data, "shop.tracker-price", 50, this::giveTracker, purchaseSlot);
            case "smoke_bomb" -> purchase(player, data, "shop.smoke-bomb-price", 80, this::giveSmokeBomb, purchaseSlot);
            case "chaos_grenade" -> purchase(player, data, "shop.chaos-grenade-price", 120, this::giveChaosGrenade, purchaseSlot);
            case "dash_item" -> purchase(player, data, "shop.dash-item-price", 100, this::giveDashItem, purchaseSlot);
            case "rage_vial" -> purchase(player, data, "shop.rage-vial-price", 90, this::giveRageVial, purchaseSlot);
            case "fake_ingot" -> purchase(player, data, "shop.fake-ingot-price", 60, this::giveFakeIngot, purchaseSlot);
            case "decoy_elixir" -> purchase(player, data, "shop.decoy-elixir-price", 200, this::giveDecoyElixir, purchaseSlot);
            case "sell_ores" -> sellOres(player, data);
        }

        // Rafraîchit l'affichage au tick suivant (solde à jour)
        Bukkit.getScheduler().runTask(plugin, () -> open(player, holder.page));
    }

    private interface Rewarder {
        void give(Player player);
    }

    private void purchase(Player player, PlayerData data, String configPath, int defaultPrice, Rewarder rewarder, int slot) {
        int price = plugin.getConfig().getInt(configPath, defaultPrice);
        if (data.getCoins() < price) {
            player.sendMessage(Component.text("Tu n'as pas assez de Bounty Coins (" + (long) data.getCoins() + "/" + price + ").", NamedTextColor.RED));
            return;
        }
        data.addCoins(-price);
        rewarder.give(player);
        player.sendMessage(Component.text("Achat effectué pour " + price + " coins !", NamedTextColor.GREEN));
        playPurchaseAnimation(player);

        if (!data.isFirstPurchaseAchievement()) {
            data.setFirstPurchaseAchievement(true);
            com.bountysmp.util.AchievementUtil.show(player, "Premier Achat", "Tu as fait tes premières emplettes en Bounty Coins.");
        }
    }

    /** Petite animation/feedback visuel + sonore à l'achat. */
    private void playPurchaseAnimation(Player player) {
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

    private void giveChaosGrenade(Player player) {
        NamespacedKey chaosKey = new NamespacedKey(plugin, "bounty_chaos_grenade");
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§dGrenade du Chaos").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : lance un effet négatif").decoration(TextDecoration.ITALIC, false),
                Component.text("§7aléatoire sur le joueur touché.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(chaosKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        giveItems(player, item);
    }

    private void giveRageVial(Player player) {
        NamespacedKey rageKey = new NamespacedKey(plugin, "bounty_rage_vial");
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6Fiole de Rage").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : Force et Résistance").decoration(TextDecoration.ITALIC, false),
                Component.text("§7au feu pendant 15 secondes.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(rageKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        giveItems(player, item);
    }

    private void giveFakeIngot(Player player) {
        NamespacedKey fakeIngotKey = new NamespacedKey(plugin, "bounty_fake_ingot");
        NamespacedKey ownerKey = new NamespacedKey(plugin, "bounty_fake_ingot_owner");
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6Faux Lingot d'Or").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Laisse-le traîner : quiconque le").decoration(TextDecoration.ITALIC, false),
                Component.text("§7ramasse (sauf toi) est maudit.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(fakeIngotKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        item.setItemMeta(meta);
        giveItems(player, item);
    }

    private void giveDecoyElixir(Player player) {
        NamespacedKey decoyKey = new NamespacedKey(plugin, "bounty_decoy_elixir");
        ItemStack item = new ItemStack(Material.HONEY_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§5Élixir du Menteur").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : brouille les Trackers").decoration(TextDecoration.ITALIC, false),
                Component.text("§7pointés sur toi pendant un moment.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(decoyKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        giveItems(player, item);
    }

    private void giveDashItem(Player player) {
        NamespacedKey dashKey = new NamespacedKey(plugin, "bounty_dash_item");
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§bÉlan du Chasseur").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : propulsion rapide").decoration(TextDecoration.ITALIC, false),
                Component.text("§7en avant, sans dégâts de chute.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(dashKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        giveItems(player, item);
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
