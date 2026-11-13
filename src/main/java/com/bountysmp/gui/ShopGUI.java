package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import com.bountysmp.util.Daily;
import com.bountysmp.util.GuiTitle;
import com.bountysmp.util.Icons;
import com.bountysmp.util.Sfx;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Boutique Bounty (/shop ou via /bounty).
 * Page 0 = Kits, page 1 = Objets, page 2 = Marché Noir, page 3 = Offres du jour (promos renouvelées chaque jour).
 * Les textures/fonds viennent du resource pack Bounty SMP (voir util/Icons et util/GuiTitle).
 */
public class ShopGUI implements Listener {

    private static final int TOTAL_PAGES = 4;
    private static final int INV_SIZE = 36;

    private static final String[] GLYPHS = {GuiTitle.KITS, GuiTitle.ITEMS, GuiTitle.BLACK, GuiTitle.DEALS};
    private static final String[] FALLBACK_TITLES = {"Boutique - Kits", "Boutique - Objets", "Boutique - Marché Noir", "Offres du jour"};

    // Produits affichés sur chaque page (0 à 2) et leurs emplacements (grille régulière)
    private static final String[][] PAGE_IDS = {
            {"iron_kit", "diamond_kit", "netherite_kit", "hunter_kit"},
            {"tracker", "smoke_bomb", "rage_vial", "chaos_grenade", "dash_item", "recall_stone"},
            {"fake_ingot", "stink_bomb", "lucky_purse", "spy_eye", "pickpocket"},
    };
    private static final int[][] PAGE_SLOTS = {
            {11, 15, 20, 24},
            {10, 12, 14, 16, 20, 22},
            {11, 13, 15, 20, 24},
    };
    private static final int SELL_INFO_SLOT = 24;
    private static final int[] DEAL_SLOTS = {11, 15, 20, 24};
    private static final int DEAL_CLOCK_SLOT = 13;
    private static final int BALANCE_SLOT = 4;
    private static final int PREV_SLOT = 27;
    private static final int BACK_SLOT = 31;
    private static final int NEXT_SLOT = 35;

    private interface Rewarder {
        void give(Player player);
    }

    private record Product(String id, Material icon, Integer cmd, String name, String priceKey,
                           int defaultPrice, List<String> lore, Rewarder reward) {
    }

    private record DealOffer(Product product, int percent, int price) {
    }

    /** Retour visuel affiché dans l'inventaire (l'action bar est cachée derrière un menu ouvert). */
    private record Flash(int slot, boolean success, String text) {
    }

    /** Marqueur d'identité fiable pour l'inventaire boutique, contient la page affichée. */
    private static class ShopHolder implements InventoryHolder {
        final int page;

        ShopHolder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final BountySMP plugin;
    private final NamespacedKey shopItemKey;
    private final NamespacedKey trackerKey;
    private final NamespacedKey smokeBombKey;

    private final Map<String, Product> catalog = new LinkedHashMap<>();
    private final Map<UUID, Flash> pendingFlash = new HashMap<>();
    private String currentItemId = "";

    private long dealsCacheDay = Long.MIN_VALUE;
    private List<DealOffer> dealsCache = List.of();

    public ShopGUI(BountySMP plugin) {
        this.plugin = plugin;
        this.shopItemKey = new NamespacedKey(plugin, "bounty_shop_item");
        this.trackerKey = new NamespacedKey(plugin, "bounty_tracker");
        this.smokeBombKey = new NamespacedKey(plugin, "bounty_smoke_bomb");
        buildCatalog();
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

    // ================================================================= catalogue
    private void add(String id, Material icon, Integer cmd, String name, String priceKey, int defaultPrice,
                     List<String> lore, Rewarder reward) {
        catalog.put(id, new Product(id, icon, cmd, name, priceKey, defaultPrice, lore, reward));
    }

    private void buildCatalog() {
        int steal = plugin.getConfig().getInt("pickpocket.steal-amount", 15);
        int channel = plugin.getConfig().getInt("recall-stone.channel-seconds", 8);

        add("iron_kit", Material.IRON_CHESTPLATE, null, "§fKit Fer", "shop.iron-kit-price", 70,
                List.of("§7Armure fer complète,", "§7épée et pioche en fer."), this::giveIronKit);
        add("diamond_kit", Material.DIAMOND_CHESTPLATE, null, "§bKit Diamant", "shop.diamond-kit-price", 220,
                List.of("§7Armure diamant complète,", "§7épée et pioche en diamant."), this::giveDiamondKit);
        add("netherite_kit", Material.NETHERITE_CHESTPLATE, null, "§4Kit Netherite", "shop.netherite-kit-price", 800,
                List.of("§7Armure netherite complète,", "§7épée, hache et pommes dorées enchantées."), this::giveNetheriteKit);
        add("hunter_kit", Material.BOW, null, "§aKit Chasseur de Primes", "shop.hunter-kit-price", 120,
                List.of("§7Armure en mailles complète,", "§7arc Puissance III + Frappe I,", "§764 flèches et de quoi manger."), this::giveHunterKit);

        add("tracker", Material.ECHO_SHARD, Icons.TRACKER, "§6Tracker", "shop.tracker-price", 30,
                List.of("§7Clic droit + Maj : choisir une cible", "§7Clic droit : coordonnées approximatives",
                        "§7(imprécises, cooldown façon perle d'ender)."), this::giveTracker);
        add("smoke_bomb", Material.FIREWORK_ROCKET, Icons.SMOKE, "§bFeu d'Artifice du Fugitif", "shop.smoke-bomb-price", 45,
                List.of("§7Clic droit : invisibilité et vitesse", "§710s, aveugle les joueurs proches 2s."), this::giveSmokeBomb);
        add("rage_vial", Material.GLASS_BOTTLE, Icons.RAGE, "§6Fiole de Rage", "shop.rage-vial-price", 50,
                List.of("§7Clic droit : Force et Résistance au feu", "§7pendant 15 secondes."), this::giveRageVial);
        add("chaos_grenade", Material.SNOWBALL, Icons.CHAOS, "§dGrenade du Chaos", "shop.chaos-grenade-price", 70,
                List.of("§7Clic droit : lance une grenade qui", "§7applique un effet négatif aléatoire", "§7au joueur touché."), this::giveChaosGrenade);
        add("dash_item", Material.FEATHER, Icons.DASH, "§bÉlan du Chasseur", "shop.dash-item-price", 60,
                List.of("§7Clic droit : propulsion rapide", "§7en avant, sans dégâts de chute."), this::giveDashItem);
        add("recall_stone", Material.AMETHYST_SHARD, Icons.RECALL, "§bPierre de Rappel", "shop.recall-stone-price", 60,
                List.of("§7Clic droit : reste immobile " + channel + "s,", "§7puis retour à ton lit (ou au spawn).",
                        "§7Annulée si tu bouges ou es touché."), this::giveRecallStone);

        add("fake_ingot", Material.GOLD_NUGGET, Icons.FAKE, "§6Faux Lingot d'Or", "shop.fake-ingot-price", 35,
                List.of("§7Laisse-le traîner au sol :", "§7quiconque le ramasse (sauf toi)", "§7est maudit (lenteur + faiblesse)."), this::giveFakeIngot);
        add("stink_bomb", Material.FERMENTED_SPIDER_EYE, Icons.STINK, "§2Bombe Puante", "shop.stink-bomb-price", 40,
                List.of("§7Clic droit : lance une bombe qui", "§7donne Nausée + Poison au joueur touché."), this::giveStinkBomb);
        add("lucky_purse", Material.SUSPICIOUS_STEW, Icons.PURSE, "§6Bourse Chanceuse", "shop.lucky-purse-price", 50,
                List.of("§7Clic droit : pari sur des coins,", "§7du jackpot au coup pour rien.", "§7(objet à usage unique)."), this::giveLuckyPurse);
        add("spy_eye", Material.ENDER_EYE, Icons.SPY, "§dŒil de Judas", "shop.spy-eye-price", 40,
                List.of("§7Clic droit : révèle la direction", "§7et la distance approximative du", "§7joueur en ligne le plus proche."), this::giveSpyEye);
        add("pickpocket", Material.STRING, Icons.POUCH, "§eBourse Trouée", "shop.pickpocket-price", 70,
                List.of("§7Clic droit : vole " + steal + " coins", "§7au joueur en ligne le plus proche", "§7(objet à usage unique)."), this::givePickpocket);
    }

    private int price(Product p) {
        return plugin.getConfig().getInt(p.priceKey(), p.defaultPrice());
    }

    private Component text(String s) {
        return Component.text(s).decoration(TextDecoration.ITALIC, false);
    }

    // ================================================================= affichage
    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        int pg = Math.max(0, Math.min(TOTAL_PAGES - 1, page));
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(new ShopHolder(pg), INV_SIZE,
                GuiTitle.of(plugin, GLYPHS[pg], FALLBACK_TITLES[pg], NamedTextColor.DARK_RED));

        if (pg < 3) {
            populateCatalogPage(inv, pg);
        } else {
            populateDealsPage(inv, data);
        }

        // solde
        ItemStack balance = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balance.getItemMeta();
        balanceMeta.displayName(text("§6Ton solde"));
        balanceMeta.lore(List.of(text("§e" + (long) data.getCoins() + " Bounty Coins")));
        Icons.cmd(plugin, balanceMeta, Icons.COIN);
        balance.setItemMeta(balanceMeta);
        inv.setItem(BALANCE_SLOT, balance);

        // navigation
        if (pg > 0) {
            inv.setItem(PREV_SLOT, tagged(Icons.gui(plugin, Icons.PREV, Material.ARROW, "§7« Page précédente", null), "nav_prev"));
        }
        if (pg < TOTAL_PAGES - 1) {
            inv.setItem(NEXT_SLOT, tagged(Icons.gui(plugin, Icons.NEXT, Material.ARROW, "§7Page suivante »", null), "nav_next"));
        }
        inv.setItem(BACK_SLOT, tagged(Icons.gui(plugin, Icons.BACK, Material.BARRIER, "§7« Retour au menu", null), "back_menu"));

        player.openInventory(inv);
    }

    private ItemStack tagged(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack productItem(Product p, String shopId, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(p.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text(displayName));
        meta.lore(lore.stream().map(this::text).toList());
        if (p.cmd() != null) {
            Icons.cmd(plugin, meta, p.cmd());
        }
        meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.STRING, shopId);
        item.setItemMeta(meta);
        return item;
    }

    private void populateCatalogPage(Inventory inv, int page) {
        String[] ids = PAGE_IDS[page];
        int[] slots = PAGE_SLOTS[page];
        for (int i = 0; i < ids.length; i++) {
            Product p = catalog.get(ids[i]);
            List<String> lore = new ArrayList<>(p.lore());
            lore.add("");
            lore.add("§ePrix : " + price(p) + " coins");
            inv.setItem(slots[i], productItem(p, p.id(), p.name(), lore));
        }
        if (page == 1) {
            inv.setItem(SELL_INFO_SLOT, tagged(Icons.gui(plugin, Icons.SELL, Material.EMERALD, "§eVendre des minerais", sellInfoLore()), "sell_info"));
        }
    }

    /** Texte de l'encart "/sell" (taux lus dans la config). */
    private List<String> sellInfoLore() {
        var cfg = plugin.getConfig();
        return List.of(
                "§7Commande : §f/sell",
                "§7Dépose tes minerais puis ferme (Échap).",
                "",
                "§71 diamant = " + cfg.getInt("ore-exchange.diamond-rate", 8) + " coins",
                "§71 émeraude = " + cfg.getInt("ore-exchange.emerald-rate", 4) + " coins",
                "§71 lingot de netherite = " + cfg.getInt("ore-exchange.netherite-ingot-rate", 50) + " coins",
                "§71 lingot d'or = " + cfg.getInt("ore-exchange.gold-ingot-rate", 2) + " coins",
                "§71 lingot de fer = " + cfg.getInt("ore-exchange.iron-ingot-rate", 1) + " coin");
    }

    // ================================================================= offres du jour
    private List<DealOffer> todayDeals() {
        long day = Daily.today(plugin);
        if (day != dealsCacheDay) {
            List<Product> pool = new ArrayList<>();
            for (Product p : catalog.values()) {
                if (!p.id().equals("netherite_kit")) {
                    pool.add(p);
                }
            }
            Random rnd = new Random(day * 7919L + 31L);
            Collections.shuffle(pool, rnd);
            int min = plugin.getConfig().getInt("deals.min-discount", 25);
            int max = Math.max(min, plugin.getConfig().getInt("deals.max-discount", 50));
            List<DealOffer> out = new ArrayList<>();
            for (int i = 0; i < DEAL_SLOTS.length && i < pool.size(); i++) {
                Product p = pool.get(i);
                int percent = min + 5 * rnd.nextInt((max - min) / 5 + 1);
                int discounted = Math.max(1, (int) Math.round(price(p) * (100 - percent) / 100.0));
                out.add(new DealOffer(p, percent, discounted));
            }
            dealsCache = out;
            dealsCacheDay = day;
        }
        return dealsCache;
    }

    private void populateDealsPage(Inventory inv, PlayerData data) {
        List<DealOffer> deals = todayDeals();
        long day = Daily.today(plugin);
        boolean fresh = data.getDealsEpochDay() == day;

        for (int i = 0; i < deals.size(); i++) {
            DealOffer offer = deals.get(i);
            Product p = offer.product();
            boolean bought = fresh && data.getDealsBought().contains(p.id());
            ItemStack item;
            if (bought) {
                item = tagged(Icons.gui(plugin, Icons.LOCKED, Material.BARRIER, "§7Offre épuisée",
                        List.of("§c✘ Déjà acheté aujourd'hui", "§7Reviens demain !")), "deal:" + i);
            } else {
                List<String> lore = new ArrayList<>(p.lore());
                lore.add("");
                lore.add("§8Prix normal : §m" + price(p) + " coins");
                lore.add("§aPrix du jour : §e" + offer.price() + " coins");
                lore.add("§7Limite : 1 achat par jour");
                item = productItem(p, "deal:" + i, p.name() + " §c(-" + offer.percent() + "%)", lore);
            }
            inv.setItem(DEAL_SLOTS[i], item);
        }
        inv.setItem(DEAL_CLOCK_SLOT, Icons.gui(plugin, Icons.CLOCK, Material.CLOCK, "§eNouvelles offres dans",
                List.of("§f" + Daily.untilReset(plugin))));
    }

    private void buyDeal(Player player, PlayerData data, int index, int slot) {
        List<DealOffer> deals = todayDeals();
        if (index < 0 || index >= deals.size()) {
            return;
        }
        long day = Daily.today(plugin);
        if (data.getDealsEpochDay() != day) {
            data.resetDeals(day);
        }
        DealOffer offer = deals.get(index);
        if (data.getDealsBought().contains(offer.product().id())) {
            player.sendActionBar(Component.text("Tu as déjà acheté cette offre aujourd'hui.", NamedTextColor.RED));
            Sfx.denied(player);
            pendingFlash.put(player.getUniqueId(), new Flash(slot, false, "§c✘ Déjà acheté aujourd'hui"));
            return;
        }
        currentItemId = offer.product().id();
        if (purchaseAt(player, data, offer.price(), offer.product().reward(), slot)) {
            data.getDealsBought().add(offer.product().id());
        }
    }

    // ================================================================= clics
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
            Sfx.click(player);
            open(player, holder.page - 1);
            return;
        }
        if (id.equals("nav_next")) {
            Sfx.click(player);
            open(player, holder.page + 1);
            return;
        }
        if (id.equals("back_menu")) {
            Sfx.click(player);
            plugin.getBountyMenuGUI().open(player);
            return;
        }

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        int slot = event.getSlot();

        if (id.startsWith("deal:")) {
            try {
                buyDeal(player, data, Integer.parseInt(id.substring(5)), slot);
            } catch (NumberFormatException ignored) {
                return;
            }
        } else {
            Product product = catalog.get(id);
            if (product != null) {
                currentItemId = id;
                purchaseAt(player, data, price(product), product.reward(), slot);
            }
        }

        // Rafraîchit l'affichage au tick suivant (solde à jour) puis fait clignoter l'objet cliqué
        Flash flash = pendingFlash.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            open(player, holder.page);
            if (flash != null) {
                applyFlash(player, flash);
            }
        });
    }

    /** @return vrai si l'achat a réussi. */
    private boolean purchaseAt(Player player, PlayerData data, int price, Rewarder rewarder, int slot) {
        if (data.getCoins() < price) {
            player.sendActionBar(Component.text("Pas assez de Bounty Coins (" + (long) data.getCoins() + "/" + price + ").", NamedTextColor.RED));
            Sfx.denied(player);
            pendingFlash.put(player.getUniqueId(), new Flash(slot, false, "§c✘ Pas assez de coins (" + (long) data.getCoins() + "/" + price + ")"));
            return false;
        }
        data.addCoins(-price);
        // message AVANT la remise des objets : si l'inventaire est plein, l'avertissement qui suit reste visible
        player.sendActionBar(Component.text("Achat effectué : -" + price + " coins", NamedTextColor.GREEN));
        rewarder.give(player);
        Sfx.shopSuccess(plugin, player, currentItemId);
        player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getEyeLocation(), 12, 0.3, 0.3, 0.3, 0);
        pendingFlash.put(player.getUniqueId(), new Flash(slot, true, "§a✔ Acheté ! §7(-" + price + " coins)"));

        if (!data.isFirstPurchaseAchievement()) {
            data.setFirstPurchaseAchievement(true);
            plugin.getAchievementUtil().awardFirstPurchase(player);
        }
        return true;
    }

    /** Remplace brièvement l'objet cliqué par une vitre verte/rouge pour confirmer l'achat ou signaler l'échec. */
    private void applyFlash(Player player, Flash flash) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof ShopHolder)) return;

        ItemStack original = top.getItem(flash.slot());
        ItemStack pane = new ItemStack(flash.success() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(text(flash.text()));
        pane.setItemMeta(meta);
        top.setItem(flash.slot(), pane);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Inventory current = player.getOpenInventory().getTopInventory();
            if (current.equals(top)) {
                current.setItem(flash.slot(), original);
            }
        }, 30L);
    }

    // ================================================================= remise des objets
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
            player.sendActionBar(Component.text("⚠ Ton inventaire était plein, certains objets sont tombés au sol.", NamedTextColor.YELLOW));
        }
    }

    /** Objet custom : matériau de base du pack + CustomModelData + marqueur interne (clé PDC). */
    private ItemStack customItem(Material material, int cmd, String name, List<String> lore, String keyName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text(name));
        meta.lore(lore.stream().map(this::text).toList());
        Icons.cmd(plugin, meta, cmd);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, keyName), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void giveTracker(Player player) {
        giveItems(player, customItem(Material.ECHO_SHARD, Icons.TRACKER, "§6Tracker de Prime",
                List.of("§7Clic droit + Maj : choisir une cible", "§7Clic droit : coordonnées approximatives."), "bounty_tracker"));
    }

    private void giveSmokeBomb(Player player) {
        giveItems(player, customItem(Material.FIREWORK_ROCKET, Icons.SMOKE, "§bFeu d'Artifice du Fugitif",
                List.of("§7Clic droit : invisibilité et vitesse", "§7pendant 10 secondes, aveugle les joueurs", "§7proches pendant 2 secondes."), "bounty_smoke_bomb"));
    }

    private void giveChaosGrenade(Player player) {
        giveItems(player, customItem(Material.SNOWBALL, Icons.CHAOS, "§dGrenade du Chaos",
                List.of("§7Clic droit : lance un effet négatif", "§7aléatoire sur le joueur touché."), "bounty_chaos_grenade"));
    }

    private void giveRageVial(Player player) {
        giveItems(player, customItem(Material.GLASS_BOTTLE, Icons.RAGE, "§6Fiole de Rage",
                List.of("§7Clic droit : Force et Résistance", "§7au feu pendant 15 secondes."), "bounty_rage_vial"));
    }

    private void giveDashItem(Player player) {
        giveItems(player, customItem(Material.FEATHER, Icons.DASH, "§bÉlan du Chasseur",
                List.of("§7Clic droit : propulsion rapide", "§7en avant, sans dégâts de chute."), "bounty_dash_item"));
    }

    private void giveRecallStone(Player player) {
        giveItems(player, customItem(Material.AMETHYST_SHARD, Icons.RECALL, "§bPierre de Rappel",
                List.of("§7Clic droit : reste immobile, puis", "§7retour à ton lit (ou au spawn).", "§7Annulée si tu bouges ou es touché."), "bounty_recall_stone"));
    }

    private void giveFakeIngot(Player player) {
        ItemStack item = customItem(Material.GOLD_NUGGET, Icons.FAKE, "§6Faux Lingot d'Or",
                List.of("§7Laisse-le traîner : quiconque le", "§7ramasse (sauf toi) est maudit."), "bounty_fake_ingot");
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bounty_fake_ingot_owner"),
                PersistentDataType.STRING, player.getUniqueId().toString());
        item.setItemMeta(meta);
        giveItems(player, item);
    }

    private void giveStinkBomb(Player player) {
        giveItems(player, customItem(Material.FERMENTED_SPIDER_EYE, Icons.STINK, "§2Bombe Puante",
                List.of("§7Clic droit : Nausée + Poison", "§7au joueur touché."), "bounty_stink_bomb"));
    }

    private void giveLuckyPurse(Player player) {
        giveItems(player, customItem(Material.SUSPICIOUS_STEW, Icons.PURSE, "§6Bourse Chanceuse",
                List.of("§7Clic droit : gain de coins aléatoire", "§7(usage unique)."), "bounty_lucky_purse"));
    }

    private void giveSpyEye(Player player) {
        giveItems(player, customItem(Material.ENDER_EYE, Icons.SPY, "§dŒil de Judas",
                List.of("§7Clic droit : révèle la direction", "§7et distance du joueur le plus proche."), "bounty_spy_eye"));
    }

    private void givePickpocket(Player player) {
        giveItems(player, customItem(Material.STRING, Icons.POUCH, "§eBourse Trouée",
                List.of("§7Clic droit : vole des coins au joueur", "§7le plus proche (usage unique)."), "bounty_pickpocket"));
    }

    private void giveHunterKit(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(org.bukkit.enchantments.Enchantment.POWER, 3, true);
        bowMeta.addEnchant(org.bukkit.enchantments.Enchantment.PUNCH, 1, true);
        bow.setItemMeta(bowMeta);
        giveItems(player,
                new ItemStack(Material.CHAINMAIL_HELMET),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE),
                new ItemStack(Material.CHAINMAIL_LEGGINGS),
                new ItemStack(Material.CHAINMAIL_BOOTS),
                bow,
                new ItemStack(Material.ARROW, 64),
                new ItemStack(Material.COOKED_BEEF, 16));
    }

    private void giveIronKit(Player player) {
        giveItems(player,
                new ItemStack(Material.IRON_HELMET),
                new ItemStack(Material.IRON_CHESTPLATE),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.IRON_SWORD),
                new ItemStack(Material.IRON_PICKAXE));
    }

    private void giveDiamondKit(Player player) {
        giveItems(player,
                new ItemStack(Material.DIAMOND_HELMET),
                new ItemStack(Material.DIAMOND_CHESTPLATE),
                new ItemStack(Material.DIAMOND_LEGGINGS),
                new ItemStack(Material.DIAMOND_BOOTS),
                new ItemStack(Material.DIAMOND_SWORD),
                new ItemStack(Material.DIAMOND_PICKAXE));
    }

    private void giveNetheriteKit(Player player) {
        giveItems(player,
                new ItemStack(Material.NETHERITE_HELMET),
                new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.NETHERITE_LEGGINGS),
                new ItemStack(Material.NETHERITE_BOOTS),
                new ItemStack(Material.NETHERITE_SWORD),
                new ItemStack(Material.NETHERITE_AXE),
                new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
    }
}
