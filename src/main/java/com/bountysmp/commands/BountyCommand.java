package com.bountysmp.commands;

import com.bountysmp.BountySMP;
import com.bountysmp.data.DataManager;
import com.bountysmp.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BountyCommand implements CommandExecutor, TabCompleter {

    private final BountySMP plugin;

    public BountyCommand(BountySMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /bounty  -> ouvre le menu graphique (profil + raccourcis vers boutique et recherchés)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Cette commande doit être utilisée en jeu.");
                return true;
            }
            plugin.getBountyMenuGUI().open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add" -> handleAdd(sender, args);
            case "addcoins" -> handleAdminCoins(sender, args, true);
            case "removecoins" -> handleAdminCoins(sender, args, false);
            case "addadmin" -> handleAddAdmin(sender, args);
            case "reset" -> handleReset(sender, args);
            case "inv" -> handleViewInventory(sender, args);
            case "enderchest" -> handleViewEnderChest(sender, args);
            default -> sender.sendMessage(Component.text("Commande inconnue. Utilisez /bounty ou /bounty add <joueur> <montant>.", NamedTextColor.RED));
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage : /bounty add <joueur> <montant>", NamedTextColor.RED));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("Tu ne peux pas mettre une prime sur toi-même.", NamedTextColor.RED));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Montant invalide.", NamedTextColor.RED));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(Component.text("Le montant doit être positif.", NamedTextColor.RED));
            return;
        }

        DataManager dm = plugin.getDataManager();
        PlayerData senderData = dm.get(player.getUniqueId());
        if (senderData.getCoins() < amount) {
            player.sendMessage(Component.text("Tu n'as pas assez de coins (" + (long) senderData.getCoins() + ").", NamedTextColor.RED));
            return;
        }

        senderData.addCoins(-amount);
        PlayerData targetData = dm.get(target.getUniqueId());
        targetData.addContribution(player.getName(), amount);

        Bukkit.broadcast(Component.text("⚠ ", NamedTextColor.RED)
                .append(Component.text(target.getName() != null ? target.getName() : "?", NamedTextColor.GOLD))
                .append(Component.text(" est maintenant recherché ! Prime totale : ", NamedTextColor.GRAY))
                .append(Component.text((long) targetData.getBounty() + " coins", NamedTextColor.YELLOW))
                .append(Component.text(" (contrat de " + player.getName() + ")", NamedTextColor.DARK_GRAY)));
    }

    private void handleAdminCoins(CommandSender sender, String[] args, boolean add) {
        if (!sender.hasPermission("bountysmp.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage : /bounty " + (add ? "addcoins" : "removecoins") + " <joueur> <montant>", NamedTextColor.RED));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Montant invalide.", NamedTextColor.RED));
            return;
        }
        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        data.addCoins(add ? amount : -amount);
        sender.sendMessage(Component.text((add ? "Ajouté " : "Retiré ") + (long) amount + " coins "
                + (add ? "à " : "de ") + target.getName(), NamedTextColor.GREEN));
    }

    /** Ajoute (au lieu d'écraser) une prime sur un joueur, gratuitement, sans passer par les coins d'un joueur. */
    private void handleAddAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bountysmp.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage : /bounty addadmin <joueur> <montant>", NamedTextColor.RED));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Montant invalide.", NamedTextColor.RED));
            return;
        }
        if (amount <= 0) {
            sender.sendMessage(Component.text("Le montant doit être positif.", NamedTextColor.RED));
            return;
        }
        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        data.addContribution("Admin", amount);
        sender.sendMessage(Component.text("Prime de " + target.getName() + " augmentée de " + (long) amount
                + " coins (total : " + (long) data.getBounty() + ").", NamedTextColor.GREEN));
    }

    /** Réinitialise TOUTES les données du plugin (coins, primes, achats...). Nécessite une confirmation explicite. */
    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bountysmp.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2 || !args[1].equals("CONFIRMER")) {
            sender.sendMessage(Component.text("⚠ Ceci va effacer TOUTES les données du plugin (coins, primes, achats...) pour TOUS les joueurs.", NamedTextColor.RED));
            sender.sendMessage(Component.text("Tape /bounty reset CONFIRMER pour valider.", NamedTextColor.YELLOW));
            return;
        }
        plugin.getDataManager().resetAll();
        plugin.getAchievementUtil().resetAllProgress();
        sender.sendMessage(Component.text("✅ Toutes les données de BountySMP ont été réinitialisées (y compris les succès des joueurs en ligne).", NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text("⚠ Un administrateur a réinitialisé le système de primes du serveur.", NamedTextColor.DARK_RED));
    }

    /** Ouvre l'inventaire en direct d'un joueur en ligne (consultable et modifiable, façon InvSee). */
    private void handleViewInventory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bountysmp.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(Component.text("Cette commande doit être utilisée en jeu.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage : /bounty inv <joueur>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Ce joueur n'est pas en ligne.", NamedTextColor.RED));
            return;
        }
        admin.openInventory(target.getInventory());
        admin.sendMessage(Component.text("Inventaire de " + target.getName() + " ouvert.", NamedTextColor.GREEN));
    }

    /** Ouvre l'enderchest en direct d'un joueur en ligne (consultable et modifiable, façon InvSee). */
    private void handleViewEnderChest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bountysmp.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(Component.text("Cette commande doit être utilisée en jeu.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage : /bounty enderchest <joueur>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Ce joueur n'est pas en ligne.", NamedTextColor.RED));
            return;
        }
        admin.openInventory(target.getEnderChest());
        admin.sendMessage(Component.text("Enderchest de " + target.getName() + " ouvert.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subs = new ArrayList<>(List.of("add"));
        if (sender.hasPermission("bountysmp.admin")) {
            subs.addAll(List.of("addcoins", "removecoins", "addadmin", "reset", "inv", "enderchest"));
        }
        if (args.length == 1) {
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && List.of("add", "addcoins", "removecoins", "addadmin", "inv", "enderchest").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return List.of("CONFIRMER").stream().filter(s -> s.startsWith(args[1].toUpperCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
