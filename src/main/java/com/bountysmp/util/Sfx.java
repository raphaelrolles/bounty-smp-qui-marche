package com.bountysmp.util;

import com.bountysmp.BountySMP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Toute la "sound design" du plugin au même endroit : chaque action a sa petite
 * séquence de sons (superposés et décalés dans le temps) pour que ça sonne vivant.
 * Les sons d'items "physiques" sont joués dans le monde (les autres joueurs les entendent),
 * les sons d'interface ne sont entendus que par le joueur concerné.
 */
public final class Sfx {

    private Sfx() {
    }

    // ---------------------------------------------------------------- helpers
    private static void me(Player p, Sound s, float vol, float pitch) {
        if (p != null && p.isOnline()) {
            p.playSound(p.getLocation(), s, vol, pitch);
        }
    }

    private static void at(Location l, Sound s, float vol, float pitch) {
        if (l != null && l.getWorld() != null) {
            l.getWorld().playSound(l, s, vol, pitch);
        }
    }

    private static void later(BountySMP plugin, long ticks, Runnable r) {
        Bukkit.getScheduler().runTaskLater(plugin, r, ticks);
    }

    // ---------------------------------------------------------------- interface générique
    public static void click(Player p) {
        me(p, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    public static void denied(Player p) {
        me(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    public static void cooldown(Player p) {
        me(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.6f);
    }

    // ---------------------------------------------------------------- boutique
    public static void shopSuccess(BountySMP plugin, Player p, String itemId) {
        Sound special = switch (itemId) {
            case "iron_kit" -> Sound.ITEM_ARMOR_EQUIP_IRON;
            case "diamond_kit" -> Sound.ITEM_ARMOR_EQUIP_DIAMOND;
            case "netherite_kit" -> Sound.ITEM_ARMOR_EQUIP_NETHERITE;
            case "hunter_kit" -> Sound.ITEM_ARMOR_EQUIP_CHAIN;
            case "tracker" -> Sound.ITEM_LODESTONE_COMPASS_LOCK;
            case "smoke_bomb" -> Sound.ENTITY_FIREWORK_ROCKET_LAUNCH;
            case "chaos_grenade" -> Sound.BLOCK_BREWING_STAND_BREW;
            case "dash_item" -> Sound.ENTITY_PHANTOM_FLAP;
            case "rage_vial" -> Sound.ENTITY_GENERIC_DRINK;
            case "fake_ingot" -> Sound.ENTITY_ITEM_PICKUP;
            case "stink_bomb" -> Sound.ENTITY_SLIME_SQUISH;
            case "lucky_purse" -> Sound.BLOCK_NOTE_BLOCK_CHIME;
            case "spy_eye" -> Sound.ENTITY_ENDER_EYE_LAUNCH;
            case "pickpocket" -> Sound.ENTITY_ITEM_FRAME_ADD_ITEM;
            case "builder_kit" -> Sound.BLOCK_STONE_PLACE;
            case "farmer_kit" -> Sound.ITEM_HOE_TILL;
            case "explorer_kit" -> Sound.ITEM_FLINTANDSTEEL_USE;
            case "toolbox" -> Sound.BLOCK_ANVIL_USE;
            default -> Sound.ENTITY_VILLAGER_YES;
        };
        me(p, special, 0.9f, 1.1f);
        later(plugin, 3, () -> me(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f));
        later(plugin, 6, () -> me(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.9f));
    }

    // ---------------------------------------------------------------- /sell
    public static void sellOpen(Player p) {
        me(p, Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
    }

    public static void sellDone(BountySMP plugin, Player p) {
        coins(plugin, p);
        later(plugin, 9, () -> me(p, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f));
    }

    // ---------------------------------------------------------------- tracker
    public static void trackerOpen(Player p) {
        me(p, Sound.ITEM_LODESTONE_COMPASS_LOCK, 0.8f, 1.2f);
    }

    public static void trackerLocked(Player p) {
        me(p, Sound.ITEM_LODESTONE_COMPASS_LOCK, 1f, 1f);
        me(p, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.6f);
    }

    public static void trackerPing(BountySMP plugin, Player p) {
        me(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.8f);
        later(plugin, 4, () -> me(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.3f));
        later(plugin, 9, () -> me(p, Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.6f));
    }

    // ---------------------------------------------------------------- items d'évasion / mobilité
    public static void smokeBomb(BountySMP plugin, Player p) {
        at(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.8f);
        me(p, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.5f);
        later(plugin, 5, () -> {
            at(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.7f);
            at(p.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 0.6f);
        });
    }

    public static void dash(BountySMP plugin, Player p) {
        at(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.6f);
        at(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
        later(plugin, 3, () -> at(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.7f, 1.7f));
    }

    // ---------------------------------------------------------------- items de combat
    public static void grenadeThrow(Location l) {
        at(l, Sound.ENTITY_WITCH_THROW, 1f, 1.1f);
        at(l, Sound.ENTITY_SNOWBALL_THROW, 0.8f, 0.8f);
    }

    public static void grenadeHit(Location l) {
        at(l, Sound.ENTITY_SPLASH_POTION_BREAK, 1f, 1f);
        at(l, Sound.ENTITY_GENERIC_EXPLODE, 0.35f, 1.8f);
        at(l, Sound.ENTITY_WITCH_AMBIENT, 0.8f, 1.2f);
    }

    public static void stinkThrow(Location l) {
        at(l, Sound.ENTITY_EGG_THROW, 1f, 0.7f);
        at(l, Sound.ENTITY_LLAMA_SPIT, 0.8f, 0.6f);
    }

    public static void stinkHit(Location l) {
        at(l, Sound.ENTITY_SLIME_SQUISH, 1f, 0.6f);
        at(l, Sound.ENTITY_SPLASH_POTION_BREAK, 0.8f, 0.7f);
        at(l, Sound.ENTITY_PUFFER_FISH_BLOW_UP, 0.7f, 0.6f);
    }

    public static void rage(BountySMP plugin, Player p) {
        at(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1f, 0.9f);
        later(plugin, 8, () -> {
            at(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.9f, 1.2f);
            at(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.35f, 1.3f);
        });
    }

    // ---------------------------------------------------------------- marché noir
    public static void spyEye(BountySMP plugin, Player p) {
        me(p, Sound.ENTITY_ENDER_EYE_LAUNCH, 1f, 1f);
        later(plugin, 8, () -> me(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.3f));
        later(plugin, 14, () -> me(p, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 1f));
    }

    public static void coins(BountySMP plugin, Player p) {
        me(p, Sound.ENTITY_ITEM_PICKUP, 1f, 1.6f);
        later(plugin, 3, () -> me(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.8f));
        later(plugin, 6, () -> me(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.4f));
    }

    public static void robbed(Player p) {
        me(p, Sound.ENTITY_VILLAGER_HURT, 1f, 0.8f);
        me(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.7f);
    }

    public static void curse(Location l) {
        at(l, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.2f);
        at(l, Sound.ENTITY_WITCH_HURT, 0.8f, 0.8f);
    }

    public static void purseJackpot(BountySMP plugin, Player p) {
        me(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        later(plugin, 4, () -> me(p, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f));
        later(plugin, 8, () -> me(p, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1.3f));
        later(plugin, 12, () -> me(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f));
    }

    public static void purseWin(BountySMP plugin, Player p) {
        me(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        later(plugin, 4, () -> me(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f));
    }

    public static void purseEmpty(BountySMP plugin, Player p) {
        me(p, Sound.ENTITY_VILLAGER_NO, 1f, 0.9f);
        later(plugin, 6, () -> me(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f));
    }

    // ---------------------------------------------------------------- kills & primes
    public static void killReward(Player killer) {
        me(killer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    /** Le tueur reçoit une fanfare, et tout le serveur entend une cloche au loin. */
    public static void bountyClaimed(BountySMP plugin, Player killer) {
        me(killer, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        later(plugin, 6, () -> coins(plugin, killer));
        for (Player online : Bukkit.getOnlinePlayers()) {
            me(online, Sound.BLOCK_BELL_USE, 0.6f, 0.8f);
        }
    }

    /** Grondement lointain pour tout le monde quand un joueur devient plus dangereux. */
    public static void danger() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            me(online, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.25f, 1.2f);
        }
    }
}
