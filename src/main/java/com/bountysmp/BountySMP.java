package com.bountysmp;

import com.bountysmp.commands.BountyCommand;
import com.bountysmp.data.DataManager;
import com.bountysmp.gui.BountyMenuGUI;
import com.bountysmp.gui.ShopGUI;
import com.bountysmp.gui.WantedGUI;
import com.bountysmp.listeners.ChaosGrenadeListener;
import com.bountysmp.listeners.DashItemListener;
import com.bountysmp.listeners.PlayerDeathListener;
import com.bountysmp.listeners.SmokeBombListener;
import com.bountysmp.listeners.TrackerListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class BountySMP extends JavaPlugin {

    private static BountySMP instance;

    private DataManager dataManager;
    private ShopGUI shopGUI;
    private WantedGUI wantedGUI;
    private BountyMenuGUI bountyMenuGUI;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.dataManager = new DataManager(getDataFolder(), getLogger());
        this.dataManager.load();

        this.shopGUI = new ShopGUI(this);
        this.wantedGUI = new WantedGUI(this);
        this.bountyMenuGUI = new BountyMenuGUI(this);

        // Commandes
        BountyCommand bountyCommand = new BountyCommand(this);
        getCommand("bounty").setExecutor(bountyCommand);
        getCommand("bounty").setTabCompleter(bountyCommand);

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager().registerEvents(wantedGUI, this);
        getServer().getPluginManager().registerEvents(bountyMenuGUI, this);
        getServer().getPluginManager().registerEvents(new TrackerListener(this), this);
        getServer().getPluginManager().registerEvents(new SmokeBombListener(this), this);
        getServer().getPluginManager().registerEvents(new ChaosGrenadeListener(this), this);
        getServer().getPluginManager().registerEvents(new DashItemListener(this), this);

        // Sauvegarde périodique (toutes les 5 minutes) pour éviter les pertes de données
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> dataManager.save(), 20L * 60 * 5, 20L * 60 * 5);

        getLogger().info("BountySMP activé - que la chasse commence !");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.save();
        }
        getLogger().info("BountySMP désactivé, données sauvegardées.");
    }

    public static BountySMP getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    public WantedGUI getWantedGUI() {
        return wantedGUI;
    }

    public BountyMenuGUI getBountyMenuGUI() {
        return bountyMenuGUI;
    }
}
