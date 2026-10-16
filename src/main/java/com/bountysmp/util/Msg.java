package com.bountysmp.util;

import com.bountysmp.BountySMP;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Messages affichés au-dessus de la barre d'XP (action bar) au lieu du chat.
 */
public final class Msg {

    private Msg() {
    }

    /**
     * L'action bar disparaît au bout de ~3 s : on la renvoie toutes les 2 s pour
     * qu'une info importante (ex. des coordonnées) reste lisible plus longtemps.
     */
    public static void sustain(BountySMP plugin, Player player, Component message, int seconds) {
        player.sendActionBar(message);
        final int repeats = Math.max(0, seconds / 2 - 1);
        if (repeats == 0) {
            return;
        }
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (!player.isOnline() || count++ >= repeats) {
                    cancel();
                    return;
                }
                player.sendActionBar(message);
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }
}
