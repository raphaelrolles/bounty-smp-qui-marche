package com.bountysmp.commands;

import com.bountysmp.BountySMP;
import com.bountysmp.data.PlayerData;
import com.bountysmp.util.Sfx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/** /pay <joueur> <montant> (ou /bounty pay ...) : envoie des Bounty Coins à un autre joueur. */
public class PayCommand implements CommandExecutor, TabCompleter {

    private final BountySMP plugin;

    public PayCommand(BountySMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        transfer(plugin, sender, args.length > 0 ? args[0] : null, args.length > 1 ? args[1] : null);
        return true;
    }

    /** Logique commune à /pay et /bounty pay. */
    public static void transfer(BountySMP plugin, CommandSender sender, String targetName, String amountText) {
        if (!(sender instanceof Player from)) {
            sender.sendMessage(Component.text("Cette commande doit être utilisée en jeu.", NamedTextColor.RED));
            return;
        }
        if (targetName == null || amountText == null) {
            from.sendMessage(Component.text("Usage : /pay <joueur> <montant>", NamedTextColor.RED));
            return;
        }
        OfflinePlayer to = Bukkit.getOfflinePlayer(targetName);
        if (!to.isOnline() && !to.hasPlayedBefore()) {
            from.sendMessage(Component.text("Joueur introuvable : " + targetName, NamedTextColor.RED));
            return;
        }
        if (to.getUniqueId().equals(from.getUniqueId())) {
            from.sendMessage(Component.text("Tu ne peux pas t'envoyer des coins à toi-même.", NamedTextColor.RED));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(amountText);
        } catch (NumberFormatException e) {
            from.sendMessage(Component.text("Montant invalide : " + amountText, NamedTextColor.RED));
            return;
        }
        int min = Math.max(1, plugin.getConfig().getInt("pay.min-amount", 1));
        if (amount < min) {
            from.sendMessage(Component.text("Montant minimum : " + min + " coin(s).", NamedTextColor.RED));
            return;
        }
        PlayerData fromData = plugin.getDataManager().get(from.getUniqueId());
        if (fromData.getCoins() < amount) {
            from.sendMessage(Component.text("Tu n'as pas assez de coins (" + (long) fromData.getCoins() + "/" + amount + ").", NamedTextColor.RED));
            return;
        }

        int taxPercent = Math.max(0, Math.min(100, plugin.getConfig().getInt("pay.tax-percent", 0)));
        int tax = (int) Math.floor(amount * taxPercent / 100.0);
        int received = amount - tax;

        PlayerData toData = plugin.getDataManager().get(to.getUniqueId());
        fromData.addCoins(-amount);
        toData.addCoins(received);

        String shownName = to.getName() != null ? to.getName() : targetName;
        from.sendActionBar(Component.text("💸 Envoyé : ", NamedTextColor.GOLD)
                .append(Component.text(amount + " coins", NamedTextColor.YELLOW))
                .append(Component.text(" à " + shownName + (tax > 0 ? " (taxe : " + tax + ")" : ""), NamedTextColor.GRAY)));
        Sfx.coins(plugin, from);

        Player receiver = Bukkit.getPlayer(to.getUniqueId());
        if (receiver != null) {
            receiver.sendActionBar(Component.text("💰 " + from.getName() + " t'a envoyé ", NamedTextColor.GOLD)
                    .append(Component.text(received + " coins", NamedTextColor.YELLOW)));
            Sfx.coins(plugin, receiver);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> !n.equals(sender.getName()) && n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
