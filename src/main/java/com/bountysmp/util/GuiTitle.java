package com.bountysmp.util;

import com.bountysmp.BountySMP;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Titres de menus : avec le resource pack, le "titre" est en réalité un glyphe de la police
 * bountysmp:gui qui affiche tout le fond du menu (voir README du pack).
 * Sans pack (resource-pack.enabled: false), on retombe sur un titre texte classique.
 */
public final class GuiTitle {

    public static final Key FONT = Key.key("bountysmp", "gui");

    public static final String MAIN = "\uE000";
    public static final String KITS = "\uE001";
    public static final String ITEMS = "\uE002";
    public static final String BLACK = "\uE003";
    public static final String DEALS = "\uE004";
    public static final String WANTED = "\uE005";
    public static final String TOP = "\uE006";
    public static final String QUESTS = "\uE007";
    public static final String SELL = "\uE008";
    public static final String REEL = "\uE009";
    public static final String THEME_MENU = "\uE011";

    private GuiTitle() {
    }

    public static Component of(BountySMP plugin, String glyph, String fallbackText, NamedTextColor fallbackColor) {
        if (!Icons.enabled(plugin)) {
            return Component.text(fallbackText, fallbackColor);
        }
        // \uF808 décale le glyphe de 8 px vers la gauche pour qu'il colle au bord du menu ; blanc = pas de teinte
        return Component.text("\uF808" + glyph).font(FONT).color(NamedTextColor.WHITE);
    }
}
