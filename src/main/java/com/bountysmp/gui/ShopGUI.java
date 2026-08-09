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
 * Page 0 = Kits (Kit Fer, Kit Diamant, Kit Netherite)
 * Page 1 = Objets (Tracker, Feu d'artifice, Fiole de Rage, Grenade du Chaos, Élan du Chasseur, Vendre des minerais)
 * Page 2 = Marché Noir (Faux Lingot d'Or, Bombe Puante, Bourse Chanceuse, Œil de Judas, Bourse Trouée)
 * Page 3 = Départ (aide au early-game : kit de survie, provisions, torches, soins)
 * Les objets sont répartis à distances égales dans une grille 4 rangées.
 */
public class ShopGUI implements Listener {

    private static final int TOTAL_PAGES = 4;
    private static final int INV_SIZE = 36;

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
            case 2 -> "Boutique - Marché Noir";
            default -> "Boutique - Départ";
        };
        Inventory inv = Bukkit.createInventory(new ShopHolder(page), INV_SIZE, Component.text(title, NamedTextColor.DARK_RED));

        if (page == 0) {
            populateKitsPage(inv, data);
        } else if (page == 1) {
            populateItemsPage(inv, data);
        } else if (page == 2) {
            populateBlackMarketPage(inv, data);
        } else {
            populateEarlyGamePage(inv, data);
        }

        // Solde du joueur
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        balanceMeta.displayName(Component.text("§6Ton solde").decoration(TextDecoration.ITALIC, false));
        balanceMeta.lore(List.of(Component.text("§e" + (long) data.getCoins() + " Bounty Coins").decoration(TextDecoration.ITALIC, false)));
        balanceItem.setItemMeta(balanceMeta);
        inv.setItem(4, balanceItem);

        // Navigation (dernière rangée, espacée régulièrement)
        if (page > 0) {
            inv.setItem(27, buildNavItem(Material.ARROW, "§7« Page précédente", "nav_prev"));
        }
        if (page < TOTAL_PAGES - 1) {
            inv.setItem(35, buildNavItem(Material.ARROW, "§7Page suivante »", "nav_next"));
        }
        inv.setItem(31, buildNavItem(Material.BARRIER, "§7« Retour au menu", "back_menu"));

        player.openInventory(inv);
    }

    private void populateKitsPage(Inventory inv, PlayerData data) {
        int ironPrice = plugin.getConfig().getInt("shop.iron-kit-price", 70);
        int diamondPrice = plugin.getConfig().getInt("shop.diamond-kit-price", 220);
        int netheritePrice = plugin.getConfig().getInt("shop.netherite-kit-price", 800);

        // Rangée 1 : 3 items également espacés (slots 10, 13, 16)
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
        int trackerPrice = plugin.getConfig().getInt("shop.tracker-price", 30);
        int smokePrice = plugin.getConfig().getInt("shop.smoke-bomb-price", 45);
        int chaosPrice = plugin.getConfig().getInt("shop.chaos-grenade-price", 70);
        int dashPrice = plugin.getConfig().getInt("shop.dash-item-price", 60);
        int ragePrice = plugin.getConfig().getInt("shop.rage-vial-price", 50);

        // Rangée 1 : 3 items également espacés (slots 10, 13, 16)
        inv.setItem(10, buildShopItem(Material.COMPASS, "tracker",
                "§6Tracker", trackerPrice,
                List.of("§7Clic droit + Maj : choisir une cible", "§7Clic droit : coordonnées approximatives",
                        "§7(imprécises, cooldown façon perle d'ender).", "", "§ePrix : " + trackerPrice + " coins")));

        inv.setItem(13, buildShopItem(Material.FIREWORK_ROCKET, "smoke_bomb",
                "§bFeu d'Artifice du Fugitif", smokePrice,
                List.of("§7Clic droit : invisibilité et vitesse", "§710s, aveugle les joueurs proches 2s.",
                        "", "§ePrix : " + smokePrice + " coins")));

        inv.setItem(16, buildShopItem(Material.POTION, "rage_vial",
                "§6Fiole de Rage", ragePrice,
                List.of("§7Clic droit : Force et Résistance au feu", "§7pendant 15 secondes.",
                        "", "§ePrix : " + ragePrice + " coins")));

        // Rangée 2 : 3 items également espacés (slots 19, 22, 25)
        inv.setItem(19, buildShopItem(Material.SNOWBALL, "chaos_grenade",
                "§dGrenade du Chaos", chaosPrice,
                List.of("§7Clic droit : lance une grenade qui", "§7applique un effet négatif aléatoire",
                        "§7au joueur touché.", "", "§ePrix : " + chaosPrice + " coins")));

        inv.setItem(22, buildShopItem(Material.FEATHER, "dash_item",
                "§bÉlan du Chasseur", dashPrice,
                List.of("§7Clic droit : propulsion rapide", "§7en avant, sans dégâts de chute.",
                        "", "§ePrix : " + dashPrice + " coins")));

        inv.setItem(25, buildShopItem(Material.HOPPER, "sell_info", "§eVendre des minerais", 0,
                List.of("§7Utilise la commande §f/sell", "§7pour échanger tes minerais", "§7contre des Bounty Coins.")));
    }

    private void populateBlackMarketPage(Inventory inv, PlayerData data) {
        int ingotPrice = plugin.getConfig().getInt("shop.fake-ingot-price", 35);
        int stinkPrice = plugin.getConfig().getInt("shop.stink-bomb-price", 40);
        int spyPrice = plugin.getConfig().getInt("shop.spy-eye-price", 40);
        int pickpocketPrice = plugin.getConfig().getInt("shop.pickpocket-price", 70);
        int pickpocketAmount = plugin.getConfig().getInt("pickpocket.steal-amount", 15);
        int pursePrice = plugin.getConfig().getInt("shop.lucky-purse-price", 50);

        // Grille également espacée (slots 10, 13, 16 puis 19, 22, 25)
        inv.setItem(10, buildShopItem(Material.GOLD_NUGGET, "fake_ingot",
                "§6Faux Lingot d'Or", ingotPrice,
                List.of("§7Laisse-le traîner au sol :", "§7quiconque le ramasse (sauf toi)",
                        "§7est maudit (lenteur + faiblesse).", "", "§ePrix : " + ingotPrice + " coins")));

        inv.setItem(13, buildShopItem(Material.FERMENTED_SPIDER_EYE, "stink_bomb",
                "§2Bombe Puante", stinkPrice,
                List.of("§7Clic droit : lance une bombe qui", "§7donne Nausée + Poison au joueur touché.",
                        "", "§ePrix : " + stinkPrice + " coins")));

        inv.setItem(16, buildShopItem(Material.SUSPICIOUS_STEW, "lucky_purse",
                "§6Bourse Chanceuse", pursePrice,
                List.of("§7Clic droit : pari sur des coins,", "§7du jackpot au coup pour rien.",
                        "§7(objet à usage unique).", "", "§ePrix : " + pursePrice + " coins")));

        inv.setItem(19, buildShopItem(Material.ENDER_EYE, "spy_eye",
                "§dŒil de Judas", spyPrice,
                List.of("§7Clic droit : révèle la direction", "§7et la distance approximative du",
                        "§7joueur en ligne le plus proche.", "", "§ePrix : " + spyPrice + " coins")));

        inv.setItem(22, buildShopItem(Material.STRING, "pickpocket",
                "§eBourse Trouée", pickpocketPrice,
                List.of("§7Clic droit : vole " + pickpocketAmount + " coins", "§7au joueur en ligne le plus proche",
                        "§7(objet à usage unique).", "", "§ePrix : " + pickpocketPrice + " coins")));
    }

    private void populateEarlyGamePage(Inventory inv, PlayerData data) {
        int kitPrice = plugin.getConfig().getInt("shop.settler-kit-price", 40);
        int enchantPrice = plugin.getConfig().getInt("shop.enchant-pack-price", 60);
        int buildPrice = plugin.getConfig().getInt("shop.building-pack-price", 25);
        int xpPrice = plugin.getConfig().getInt("shop.xp-bottles-price", 30);

        // Rangée également espacée (slots 10, 13, 16, 19)
        inv.setItem(10, buildShopItem(Material.IRON_SWORD, "settler_kit",
                "§fTrousse du Colon", kitPrice,
                List.of("§7Outils et épée en fer, bouclier,", "§716 torches et 16 pains.",
                        "", "§ePrix : " + kitPrice + " coins")));

        inv.setItem(13, buildShopItem(Material.ENCHANTING_TABLE, "enchant_pack",
                "§dPack d'Enchantement", enchantPrice,
                List.of("§7Table d'enchantement,", "§74 lapis-lazuli et 8 fioles", "§7d'expérience.",
                        "", "§ePrix : " + enchantPrice + " coins")));

        inv.setItem(16, buildShopItem(Material.COBBLESTONE, "building_pack",
                "§7Sac de Construction", buildPrice,
                List.of("§764 pierres, 64 terre", "§7et un seau d'eau pour bâtir vite.",
                        "", "§ePrix : " + buildPrice + " coins")));

        inv.setItem(19, buildShopItem(Material.EXPERIENCE_BOTTLE, "xp_bottles",
                "§aFioles d'Expérience", xpPrice,
                List.of("§716 fioles d'expérience", "§7pour monter de niveau vite.",
                        "", "§ePrix : " + xpPrice + " coins")));
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
            case "iron_kit" -> purchase(player, data, "shop.iron-kit-price", 70, this::giveIronKit, purchaseSlot);
            case "diamond_kit" -> purchase(player, data, "shop.diamond-kit-price", 220, this::giveDiamondKit, purchaseSlot);
            case "netherite_kit" -> purchase(player, data, "shop.netherite-kit-price", 800, this::giveNetheriteKit, purchaseSlot);
            case "tracker" -> purchase(player, data, "shop.tracker-price", 30, this::giveTracker, purchaseSlot);
            case "smoke_bomb" -> purchase(player, data, "shop.smoke-bomb-price", 45, this::giveSmokeBomb, purchaseSlot);
            case "chaos_grenade" -> purchase(player, data, "shop.chaos-grenade-price", 70, this::giveChaosGrenade, purchaseSlot);
            case "dash_item" -> purchase(player, data, "shop.dash-item-price", 60, this::giveDashItem, purchaseSlot);
            case "rage_vial" -> purchase(player, data, "shop.rage-vial-price", 50, this::giveRageVial, purchaseSlot);
            case "fake_ingot" -> purchase(player, data, "shop.fake-ingot-price", 35, this::giveFakeIngot, purchaseSlot);
            case "stink_bomb" -> purchase(player, data, "shop.stink-bomb-price", 40, this::giveStinkBomb, purchaseSlot);
            case "lucky_purse" -> purchase(player, data, "shop.lucky-purse-price", 50, this::giveLuckyPurse, purchaseSlot);
            case "spy_eye" -> purchase(player, data, "shop.spy-eye-price", 40, this::giveSpyEye, purchaseSlot);
            case "pickpocket" -> purchase(player, data, "shop.pickpocket-price", 70, this::givePickpocket, purchaseSlot);
            case "settler_kit" -> purchase(player, data, "shop.settler-kit-price", 40, this::giveSettlerKit, purchaseSlot);
            case "enchant_pack" -> purchase(player, data, "shop.enchant-pack-price", 60, this::giveEnchantPack, purchaseSlot);
            case "building_pack" -> purchase(player, data, "shop.building-pack-price", 25, this::giveBuildingPack, purchaseSlot);
            case "xp_bottles" -> purchase(player, data, "shop.xp-bottles-price", 30, this::giveXpBottles, purchaseSlot);
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

    private void giveStinkBomb(Player player) {
        NamespacedKey stinkKey = new NamespacedKey(plugin, "bounty_stink_bomb");
        ItemStack item = new ItemStack(Material.FERMENTED_SPIDER_EYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§2Bombe Puante").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : Nausée + Poison").decoration(TextDecoration.ITALIC, false),
                Component.text("§7au joueur touché.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(stinkKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        giveItems(player, item);
    }

    private void giveLuckyPurse(Player player) {
        NamespacedKey purseKey = new NamespacedKey(plugin, "bounty_lucky_purse");
        ItemStack item = new ItemStack(Material.SUSPICIOUS_STEW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6Bourse Chanceuse").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : gain de coins aléatoire").decoration(TextDecoration.ITALIC, false),
                Component.text("§7(usage unique).").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(purseKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        giveItems(player, item);
    }

    private void giveSettlerKit(Player player) {
        giveItems(player,
                new ItemStack(Material.IRON_SWORD),
                new ItemStack(Material.IRON_PICKAXE),
                new ItemStack(Material.IRON_AXE),
                new ItemStack(Material.SHIELD),
                new ItemStack(Material.TORCH, 16),
                new ItemStack(Material.BREAD, 16)
        );
    }

    private void giveEnchantPack(Player player) {
        giveItems(player,
                new ItemStack(Material.ENCHANTING_TABLE),
                new ItemStack(Material.LAPIS_LAZULI, 4),
                new ItemStack(Material.EXPERIENCE_BOTTLE, 8)
        );
    }

    private void giveBuildingPack(Player player) {
        giveItems(player,
                new ItemStack(Material.COBBLESTONE, 64),
                new ItemStack(Material.DIRT, 64),
                new ItemStack(Material.WATER_BUCKET)
        );
    }

    private void giveXpBottles(Player player) {
        giveItems(player, new ItemStack(Material.EXPERIENCE_BOTTLE, 16));
    }

    private void giveSpyEye(Player player) {
        NamespacedKey spyKey = new NamespacedKey(plugin, "bounty_spy_eye");
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§dŒil de Judas").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : révèle la direction").decoration(TextDecoration.ITALIC, false),
                Component.text("§7et distance du joueur le plus proche.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(spyKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        giveItems(player, item);
    }

    private void givePickpocket(Player player) {
        NamespacedKey pickpocketKey = new NamespacedKey(plugin, "bounty_pickpocket");
        ItemStack item = new ItemStack(Material.STRING);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§eBourse Trouée").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit : vole des coins au joueur").decoration(TextDecoration.ITALIC, false),
                Component.text("§7le plus proche (usage unique).").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(pickpocketKey, PersistentDataType.BYTE, (byte) 1);
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
