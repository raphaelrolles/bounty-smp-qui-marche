package com.bountysmp.util;

import org.bukkit.OfflinePlayer;

/** Petits utilitaires sur les joueurs. */
public final class Players {

    private Players() {
    }

    /**
     * Vrai pour un vrai joueur du serveur. Attention : OfflinePlayer#hasPlayedBefore() reste FAUX pendant toute la
     * première session d'un nouveau joueur (jusqu'à sa reconnexion), donc un joueur connecté est toujours valide.
     */
    public static boolean isReal(OfflinePlayer player) {
        return player.isOnline() || (player.getName() != null && player.hasPlayedBefore());
    }
}
