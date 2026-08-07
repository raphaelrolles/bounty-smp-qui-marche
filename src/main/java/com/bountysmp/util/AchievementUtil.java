package com.bountysmp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Affiche un "succès" personnalisé façon toast Minecraft (titre + son),
 * sans dépendre du système d'advancements vanilla.
 */
public final class AchievementUtil {

    private AchievementUtil() {
    }

    public static void show(Player player, String heading, String description) {
        Title title = Title.title(
                Component.text("✦ Succès débloqué ✦", NamedTextColor.GOLD),
                Component.text(heading, NamedTextColor.YELLOW)
                        .append(Component.text(" — " + description, NamedTextColor.GRAY)),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500))
        );
        player.showTitle(title);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }
}
