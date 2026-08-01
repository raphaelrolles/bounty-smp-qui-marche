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
        DataManager dm = plugin.getDataManager();

        // /bounty  -> affiche ses propres infos
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Cette commande doit être utilisée en jeu.");
                return true;
            }
            PlayerData data = dm.get(player.getUniqueId());
            player.sendMessage(Component.text("===== Votre profil Bounty =====", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Vos coins : ", NamedTextColor.GRAY)
                    .append(Component.text((long) data.getCoins(), NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("Votre prime : ", NamedTextColor.GRAY)
                    .append(Component.text((long) data.getBounty(), NamedTextColor.RED)));
            player.sendMessage(Component.text("Titre : ", NamedTextColor.GRAY)
                    .append(Component.text(plugin.getReputationUtil().getTitle(data.getBountiesClaimed()), NamedTextColor.GOLD)));
            player.sendMessage(Component.text("Primes récupérées : ", NamedTextColor.GRAY)
                    .append(Component.text(data.getBountiesClaimed(), NamedTextColor.AQUA)));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set" -> handleSet(sender, args);
            case "shop" -> handleShop(sender);
            case "top" -> handleTop(sender);
            case "addcoins" -> handleAdminCoins(sender, args, true);
            case "removecoins" -> handleAdminCoins(sender, args, false);
            case "setadmin" -> handleSetAdmin(sender, args);
            default -> sender.sendMessage(Component.text("Commande inconnue. Utilisez /bounty, /bounty set <joueur> <montant>, /bounty shop, /bounty top.", NamedTextColor.RED));
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage : /bounty set <joueur> <montant>", NamedTextColor.RED));
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

    private void handleShop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
            return;
        }
        plugin.getShopGUI().open(player);
    }

    private void handleTop(CommandSender sender) {
        List<PlayerData> hunters = plugin.getDataManager().topHunters(10);
        sender.sendMessage(Component.text("===== TOP CHASSEURS =====", NamedTextColor.GOLD));
        if (hunters.isEmpty()) {
            sender.sendMessage(Component.text("Personne n'a encore récupéré de prime.", NamedTextColor.GRAY));
            return;
        }
        int rank = 1;
        for (PlayerData data : hunters) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(data.getUuid());
            sender.sendMessage(Component.text(rank + ". ", NamedTextColor.GRAY)
                    .append(Component.text(op.getName() != null ? op.getName() : "?", NamedTextColor.AQUA))
                    .append(Component.text(" - " + data.getBountiesClaimed() + " primes - ", NamedTextColor.GRAY))
                    .append(Component.text(plugin.getReputationUtil().getTitle(data.getBountiesClaimed()), NamedTextColor.GOLD)));
            rank++;
        }
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

    private void handleSetAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bountysmp.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage : /bounty setadmin <joueur> <montant>", NamedTextColor.RED));
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
        data.clearBounty();
        data.setBounty(amount);
        if (amount > 0) {
            data.getBountyContributors().put("Admin", amount);
        }
        sender.sendMessage(Component.text("Prime de " + target.getName() + " définie à " + (long) amount + " coins.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subs = new ArrayList<>(List.of("set", "shop", "top"));
        if (sender.hasPermission("bountysmp.admin")) {
            subs.addAll(List.of("addcoins", "removecoins", "setadmin"));
        }
        if (args.length == 1) {
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && List.of("set", "addcoins", "removecoins", "setadmin").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
