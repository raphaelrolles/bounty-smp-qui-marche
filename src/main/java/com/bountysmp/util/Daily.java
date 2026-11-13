package com.bountysmp.util;

import com.bountysmp.BountySMP;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Gestion du "jour" pour les quêtes et les offres du jour : le renouvellement se fait à minuit
 * dans le fuseau horaire configuré (daily.timezone, par défaut Europe/Paris).
 */
public final class Daily {

    private Daily() {
    }

    public static ZoneId zone(BountySMP plugin) {
        try {
            return ZoneId.of(plugin.getConfig().getString("daily.timezone", "Europe/Paris"));
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }

    public static long today(BountySMP plugin) {
        return LocalDate.now(zone(plugin)).toEpochDay();
    }

    /** Temps restant avant le prochain renouvellement, ex. "5 h 12 min". */
    public static String untilReset(BountySMP plugin) {
        ZoneId z = zone(plugin);
        ZonedDateTime now = ZonedDateTime.now(z);
        ZonedDateTime next = now.toLocalDate().plusDays(1).atStartOfDay(z);
        Duration d = Duration.between(now, next);
        return d.toHours() + " h " + d.toMinutesPart() + " min";
    }
}
