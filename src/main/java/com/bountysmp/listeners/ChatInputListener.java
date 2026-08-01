package com.bountysmp.listeners;

import com.bountysmp.BountySMP;
import com.bountysmp.data.DataManager;
import com.bountysmp.data.PlayerData;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Gère l'objet "Contrat Amélioré" : clic droit -> le joueur tape en chat
 * "<joueur> <montant>" pour créer une prime annoncée à tout le serveur (avec titre).
 */
public class ChatInputListener implements Listener {

    private final BountySMP plugin;
    private final NamespacedKey contractKey;
    private final Set<UUID> awaitingContractInput = new HashSet<>();

    public ChatInputListener(BountySMP plugin) {
        this.plugin = plugin;
        this.contractKey = new NamespacedKey(plugin, "bounty_contract");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Byte flag = meta.getPersistentDataContainer().get(contractKey, PersistentDataType.BYTE);
        if (flag == null) return;

        Player player = event.getPlayer();
        awaitingContractInput.add(player.getUniqueId());
        player.sendMessage(Component.text("Écris dans le chat : ", NamedTextColor.GOLD)
                .append(Component.text("<joueur> <montant>", NamedTextColor.YELLOW))
                .append(Component.text(" pour créer un contrat amélioré. Tape 'annuler' pour annuler.", NamedTextColor.GRAY)));

        // Consomme l'objet
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingContractInput.contains(player.getUniqueId())) return;

        event.setCancelled(true);
        awaitingContractInput.remove(player.getUniqueId());

        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        // Le reste doit s'exécuter sur le thread principal (accès Bukkit API)
        Bukkit.getScheduler().runTask(plugin, () -> handleContractInput(player, message));
    }

    private void handleContractInput(Player player, String message) {
        if (message.equalsIgnoreCase("annuler")) {
            player.sendMessage(Component.text("Contrat annulé.", NamedTextColor.GRAY));
            return;
        }

        String[] parts = message.split("\\s+");
        if (parts.length != 2) {
            player.sendMessage(Component.text("Format invalide. Le contrat a été perdu.", NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(parts[0]);
        double amount;
        try {
            amount = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Montant invalide. Le contrat a été perdu.", NamedTextColor.RED));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(Component.text("Le montant doit être positif. Le contrat a été perdu.", NamedTextColor.RED));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("Tu ne peux pas cibler toi-même. Le contrat a été perdu.", NamedTextColor.RED));
            return;
        }

        DataManager dm = plugin.getDataManager();
        PlayerData targetData = dm.get(target.getUniqueId());
        targetData.addContribution(player.getName() + " (Contrat Amélioré)", amount);

        String targetName = target.getName() != null ? target.getName() : parts[0];

        // Annonce visible à tout le serveur, avec titre à l'écran
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showTitle(Title.title(
                    Component.text("⚠ CONTRAT AMÉLIORÉ ⚠", NamedTextColor.DARK_RED),
                    Component.text(targetName + " - " + (long) amount + " coins", NamedTextColor.GOLD),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
        }
        Bukkit.broadcast(Component.text("📜 ", NamedTextColor.GOLD)
                .append(Component.text(player.getName(), NamedTextColor.AQUA))
                .append(Component.text(" a lancé un contrat amélioré sur ", NamedTextColor.GRAY))
                .append(Component.text(targetName, NamedTextColor.RED))
                .append(Component.text(" ! Prime totale : " + (long) targetData.getBounty() + " coins.", NamedTextColor.YELLOW)));
    }
}
