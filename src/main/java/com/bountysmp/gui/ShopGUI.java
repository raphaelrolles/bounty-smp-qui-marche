package com.bountysmp.gui;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
 * Vend : Tracker (compass), Contrat amélioré, Hunter Kit.
 */
public class ShopGUI implements Listener {

    public static final String SHOP_TITLE = "Boutique Bounty";

    /** Marqueur d'identité fiable pour l'inventaire boutique (ne dépend pas du titre affiché). */
    private static class ShopHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null; // non utilisé, sert uniquement de marqueur d'identité
        }
    }

    private final BountySMP plugin;
    private final NamespacedKey shopItemKey;

    public ShopGUI(BountySMP plugin) {
        this.plugin = plugin;
        this.shopItemKey = new NamespacedKey(plugin, "bounty_shop_item");
    }

    public NamespacedKey getShopItemKey() {
        return shopItemKey;
    }

    public void open(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        player.sendMessage(Component.text("💰 Ton solde : ", NamedTextColor.GOLD)
                .append(Component.text((long) data.getCoins() + " Bounty Coins", NamedTextColor.YELLOW)));

        Inventory inv = org.bukkit.Bukkit.createInventory(new ShopHolder(), 27, Component.text(SHOP_TITLE, NamedTextColor.DARK_RED));

        int trackerPrice = plugin.getConfig().getInt("shop.tracker-price", 50);
        int contractPrice = plugin.getConfig().getInt("shop.contract-price", 100);
        int kitPrice = plugin.getConfig().getInt("shop.hunter-kit-price", 150);

        inv.setItem(11, buildShopItem(Material.COMPASS, "tracker",
                "§6Tracker", trackerPrice,
                List.of("§7Pointe vers un joueur", "§7recherché le plus proche.", "", "§ePrix : " + trackerPrice + " coins")));

        inv.setItem(13, buildShopItem(Material.PAPER, "contract",
                "§cContrat Amélioré", contractPrice,
                List.of("§7Crée une prime plus visible,", "§7annoncée à tout le serveur.", "", "§ePrix : " + contractPrice + " coins")));

        inv.setItem(15, buildShopItem(Material.GOLDEN_APPLE, "kit",
                "§aHunter Kit", kitPrice,
                List.of("§7Pommes dorées, nourriture,", "§7flèches et un arc.", "", "§ePrix : " + kitPrice + " coins")));

        // Item décoratif (non achetable) affichant le solde du joueur
        ItemStack balanceItem = new ItemStack(Material.SUNFLOWER);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        balanceMeta.displayName(Component.text("§6Ton solde").decoration(TextDecoration.ITALIC, false));
        balanceMeta.lore(List.of(Component.text("§e" + (long) data.getCoins() + " Bounty Coins").decoration(TextDecoration.ITALIC, false)));
        balanceItem.setItemMeta(balanceMeta);
        inv.setItem(4, balanceItem);

        player.openInventory(inv);
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

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder)) return;

        // Bloque TOUT clic dans cette vue (boutique + inventaire du joueur en dessous)
        event.setCancelled(true);

        // On ne traite que les clics sur un objet de la boutique elle-même (pas l'inventaire du joueur en bas)
        if (!(event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof ShopHolder)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;

        String id = clicked.getItemMeta().getPersistentDataContainer().get(shopItemKey, PersistentDataType.STRING);
        if (id == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        switch (id) {
            case "tracker" -> purchase(player, data, "shop.tracker-price", 50, this::giveTracker);
            case "contract" -> purchase(player, data, "shop.contract-price", 100, this::giveContract);
            case "kit" -> purchase(player, data, "shop.hunter-kit-price", 150, this::giveKit);
        }
    }

    private interface Rewarder {
        void give(Player player);
    }

    private void purchase(Player player, PlayerData data, String configPath, int defaultPrice, Rewarder rewarder) {
        int price = plugin.getConfig().getInt(configPath, defaultPrice);
        if (data.getCoins() < price) {
            player.sendMessage(Component.text("Tu n'as pas assez de Bounty Coins (" + (long) data.getCoins() + "/" + price + ").", NamedTextColor.RED));
            return;
        }
        data.addCoins(-price);
        rewarder.give(player);
        player.sendMessage(Component.text("Achat effectué pour " + price + " coins !", NamedTextColor.GREEN));
    }

    private void giveTracker(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.displayName(Component.text("§6Tracker de Prime").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit pour pointer vers").decoration(TextDecoration.ITALIC, false),
                Component.text("§7le joueur recherché le plus proche.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bounty_tracker"), PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        player.getInventory().addItem(compass);
    }

    private void giveContract(Player player) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(Component.text("§cContrat Amélioré").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("§7Clic droit pour rédiger un contrat").decoration(TextDecoration.ITALIC, false),
                Component.text("§7annoncé à tout le serveur.").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bounty_contract"), PersistentDataType.BYTE, (byte) 1);
        paper.setItemMeta(meta);
        player.getInventory().addItem(paper);
    }

    private void giveKit(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.GOLDEN_APPLE, 2),
                new ItemStack(Material.BREAD, 16),
                new ItemStack(Material.ARROW, 32),
                new ItemStack(Material.BOW, 1)
        );
    }
}
