package com.bountysmp.util;

import org.bukkit.Material;

import java.util.Locale;

/** Thèmes visuels du menu /bounty (chaque thème correspond à un fond du resource pack). */
public enum MenuTheme {
    PIRATE("Bois de pirate", "\uE000", Material.OAK_PLANKS),
    STEEL("Acier", "\uE00A", Material.IRON_BLOCK),
    COPPER("Cuivre", "\uE00B", Material.COPPER_BLOCK),
    BLACK("Marché Noir", "\uE00C", Material.AMETHYST_BLOCK),
    CRIMSON("Cramoisi", "\uE00D", Material.RED_WOOL),
    NIGHT("Nuit", "\uE00E", Material.LAPIS_BLOCK),
    PARCHMENT("Parchemin", "\uE00F", Material.SMOOTH_SANDSTONE),
    EMERALD("Émeraude", "\uE010", Material.EMERALD_BLOCK);

    private final String label;
    private final String glyph;
    private final Material icon;

    MenuTheme(String label, String glyph, Material icon) {
        this.label = label;
        this.glyph = glyph;
        this.icon = icon;
    }

    public String label() {
        return label;
    }

    public String glyph() {
        return glyph;
    }

    public Material icon() {
        return icon;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MenuTheme from(String id) {
        if (id != null) {
            for (MenuTheme t : values()) {
                if (t.id().equals(id)) {
                    return t;
                }
            }
        }
        return PIRATE;
    }
}
