package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gère la "Bourse Chanceuse" du Marché Noir : à usage unique, ouverture pariée
 * avec une animation de défilement façon "ouverture de caisse" (un bandeau
 * d'icônes défile puis ralentit jusqu'à s'arrêter sur le résultat).
 */
public class LuckyPurseListener implements Listener {

    private static class ReelHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    private enum Tier {
        NOTHING(Material.BARRIER, "§7Rien"),
        SMALL(Material.GOLD_NUGGET, "§ePetit gain"),
        JACKPOT(Material.DIAMOND, "§bJACKPOT");

        final Material material;
        final String label;
        Tier(Material material, String label) {
            this.material = material;
            this.label = label;
        }
    }

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

        // Consomme l'objet immédiatement (usage unique, évite le spam pendant l'animation)
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        // Détermine le résultat AVANT l'animation
        int jackpot = plugin.getConfig().getInt("lucky-purse.jackpot-reward", 300);
        int roll = ThreadLocalRandom.current().nextInt(100);
        Tier resultTier;
        int reward;
        if (roll < 10) {
            resultTier = Tier.JACKPOT;
            reward = jackpot;
        } else if (roll < 40) {
            resultTier = Tier.NOTHING;
            reward = 0;
        } else {
            resultTier = Tier.SMALL;
            reward = ThreadLocalRandom.current().nextInt(20, 80);
        }

        playCaseOpeningAnimation(player, resultTier, reward);
    }

    @EventHandler
    public void onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ReelHolder) {
            event.setCancelled(true);
        }
    }

    /** Animation façon "ouverture de caisse" : un bandeau d'icônes défile et ralentit jusqu'à s'arrêter sur le résultat. */
    private void playCaseOpeningAnimation(Player player, Tier resultTier, int reward) {
        Inventory inv = Bukkit.createInventory(new ReelHolder(), 9, Component.text("🎰 Bourse Chanceuse", NamedTextColor.GOLD));

        int frames = 26;
        int reelLength = frames + 9;
        Tier[] reel = new Tier[reelLength];
        for (int i = 0; i < reelLength; i++) {
            reel[i] = randomTier();
        }
        int targetPos = frames + 3; // position qui atterrit au centre (slot 4) à la dernière frame
        reel[targetPos] = resultTier;

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, tierItem(reel[i]));
        }
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);

        runFrame(player, inv, reel, 1, frames, resultTier, reward);
    }

    /** Fait défiler récursivement le bandeau, avec un délai croissant vers la fin (ralentissement). */
    private void runFrame(Player player, Inventory inv, Tier[] reel, int frame, int totalFrames, Tier resultTier, int reward) {
        // Si le joueur a fermé l'inventaire en cours de route, on continue quand même en tâche de fond
        // pour garantir que la récompense est bien attribuée à la fin.
        boolean stillViewing = player.getOpenInventory().getTopInventory().equals(inv);

        if (frame >= totalFrames) {
            int targetPos = totalFrames + 3;
            if (stillViewing) {
                for (int i = 0; i < 9; i++) {
                    inv.setItem(i, tierItem(reel[targetPos - 4 + i]));
                }
                inv.setItem(4, highlight(tierItem(resultTier)));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }
            resolveReward(player, resultTier, reward);

            if (stillViewing) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.getOpenInventory().getTopInventory().equals(inv)) {
                        player.closeInventory();
                    }
                }, 40L);
            }
            return;
        }

        if (stillViewing) {
            for (int i = 0; i < 9; i++) {
                inv.setItem(i, tierItem(reel[frame - 1 + i]));
            }
            float pitch = Math.min(2f, 0.6f + (frame / (float) totalFrames));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, pitch);
        }

        // Ralentit progressivement sur les 10 dernières frames (façon roulette qui s'arrête)
        long delay = frame > totalFrames - 10 ? (2 + (frame - (totalFrames - 10))) : 1;
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> runFrame(player, inv, reel, frame + 1, totalFrames, resultTier, reward), delay);
    }

    private void resolveReward(Player player, Tier resultTier, int reward) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (reward > 0) {
            data.addCoins(reward);
        }
        String message = switch (resultTier) {
            case JACKPOT -> "🎉 JACKPOT ! Tu trouves ";
            case NOTHING -> null;
            case SMALL -> "💰 Tu trouves ";
        };
        if (message != null) {
            player.sendMessage(Component.text(message, NamedTextColor.GOLD)
                    .append(Component.text(reward + " coins", NamedTextColor.YELLOW))
                    .append(Component.text(" dans la bourse !", NamedTextColor.GOLD)));
        } else {
            player.sendMessage(Component.text("😕 La bourse était vide, tu ne gagnes rien.", NamedTextColor.GRAY));
        }
    }

    private Tier randomTier() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 10) return Tier.JACKPOT;
        if (roll < 40) return Tier.NOTHING;
        return Tier.SMALL;
    }

    private ItemStack tierItem(Tier tier) {
        ItemStack item = new ItemStack(tier.material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(tier.label).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack highlight(ItemStack base) {
        ItemMeta meta = base.getItemMeta();
        meta.lore(List.of(Component.text("§6→ Résultat final").decoration(TextDecoration.ITALIC, false)));
        base.setItemMeta(meta);
        return base;
    }
}
