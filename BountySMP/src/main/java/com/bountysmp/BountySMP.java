package com.bountysmp;

import com.bountysmp.commands.BalanceCommand;
import com.bountysmp.commands.BountyCommand;
import com.bountysmp.commands.PayCommand;
import com.bountysmp.commands.SellCommand;
import com.bountysmp.commands.StatsCommand;
import com.bountysmp.commands.ShopCommand;
import com.bountysmp.data.DataManager;
import com.bountysmp.gui.BountyMenuGUI;
import com.bountysmp.gui.QuestGUI;
import com.bountysmp.gui.ThemeGUI;
import com.bountysmp.gui.SellGUI;
import com.bountysmp.gui.ShopGUI;
import com.bountysmp.gui.TopGUI;
import com.bountysmp.gui.WantedGUI;
import com.bountysmp.listeners.ChaosGrenadeListener;
import com.bountysmp.listeners.DashItemListener;
import com.bountysmp.listeners.FakeIngotListener;
import com.bountysmp.listeners.CombatListener;
import com.bountysmp.listeners.LuckyPurseListener;
import com.bountysmp.listeners.MerchantListener;
import com.bountysmp.listeners.PlayerDeathListener;
import com.bountysmp.listeners.PickpocketListener;
import com.bountysmp.listeners.RecallStoneListener;
import com.bountysmp.listeners.RageVialListener;
import com.bountysmp.listeners.SmokeBombListener;
import com.bountysmp.listeners.SpyEyeListener;
import com.bountysmp.listeners.StinkBombListener;
import com.bountysmp.listeners.TrackerListener;
import com.bountysmp.quests.QuestListener;
import com.bountysmp.quests.QuestManager;
import com.bountysmp.util.AchievementUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class BountySMP extends JavaPlugin {

    private static BountySMP instance;

    private DataManager dataManager;
    private ShopGUI shopGUI;
    private WantedGUI wantedGUI;
    private BountyMenuGUI bountyMenuGUI;
    private TopGUI topGUI;
    private SellGUI sellGUI;
    private AchievementUtil achievementUtil;
    private QuestManager questManager;
    private QuestGUI questGUI;
    private ThemeGUI themeGUI;
    private MerchantListener merchantListener;
    private CombatListener combatListener;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.dataManager = new DataManager(getDataFolder(), getLogger());
        this.dataManager.load();

        this.shopGUI = new ShopGUI(this);
        this.wantedGUI = new WantedGUI(this);
        this.bountyMenuGUI = new BountyMenuGUI(this);
        this.topGUI = new TopGUI(this);
        this.sellGUI = new SellGUI(this);
        this.questManager = new QuestManager(this);
        this.questGUI = new QuestGUI(this);
        this.themeGUI = new ThemeGUI(this);
        this.achievementUtil = new AchievementUtil(this);
        this.achievementUtil.registerAdvancements();

        // Commandes
        BountyCommand bountyCommand = new BountyCommand(this);
        getCommand("bounty").setExecutor(bountyCommand);
        getCommand("bounty").setTabCompleter(bountyCommand);
        getCommand("sell").setExecutor(new SellCommand(this));
        BalanceCommand balanceCommand = new BalanceCommand(this);
        getCommand("balance").setExecutor(balanceCommand);
        getCommand("balance").setTabCompleter(balanceCommand);
        PayCommand payCommand = new PayCommand(this);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);
        StatsCommand statsCommand = new StatsCommand(this);
        getCommand("stats").setExecutor(statsCommand);
        getCommand("stats").setTabCompleter(statsCommand);
        // succès "à seuil" (solde, prime) : vérifiés toutes les 30 s pour les joueurs connectés
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player online : getServer().getOnlinePlayers()) {
                achievementUtil.checkThresholds(online);
            }
        }, 200L, 600L);
        getCommand("shop").setExecutor(new ShopCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager().registerEvents(wantedGUI, this);
        getServer().getPluginManager().registerEvents(bountyMenuGUI, this);
        getServer().getPluginManager().registerEvents(topGUI, this);
        getServer().getPluginManager().registerEvents(sellGUI, this);
        getServer().getPluginManager().registerEvents(new TrackerListener(this), this);
        getServer().getPluginManager().registerEvents(new SmokeBombListener(this), this);
        getServer().getPluginManager().registerEvents(new ChaosGrenadeListener(this), this);
        getServer().getPluginManager().registerEvents(new DashItemListener(this), this);
        getServer().getPluginManager().registerEvents(new RageVialListener(this), this);
        getServer().getPluginManager().registerEvents(new FakeIngotListener(this), this);
        getServer().getPluginManager().registerEvents(new StinkBombListener(this), this);
        getServer().getPluginManager().registerEvents(new LuckyPurseListener(this), this);
        getServer().getPluginManager().registerEvents(new SpyEyeListener(this), this);
        getServer().getPluginManager().registerEvents(new PickpocketListener(this), this);
        getServer().getPluginManager().registerEvents(new RecallStoneListener(this), this);
        getServer().getPluginManager().registerEvents(questGUI, this);
        getServer().getPluginManager().registerEvents(themeGUI, this);
        this.merchantListener = new MerchantListener(this);
        getServer().getPluginManager().registerEvents(merchantListener, this);
        this.combatListener = new CombatListener(this);
        getServer().getPluginManager().registerEvents(combatListener, this);
        getServer().getScheduler().runTaskTimer(this, combatListener::tickExpiry, 20L, 20L);
        QuestListener questListener = new QuestListener(this, questManager);
        getServer().getPluginManager().registerEvents(questListener, this);
        // temps de jeu des quêtes : +1 minute active chaque minute
        getServer().getScheduler().runTaskTimer(this, questListener::tickPlaytime, 20L * 60, 20L * 60);

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

    public TopGUI getTopGUI() {
        return topGUI;
    }

    public SellGUI getSellGUI() {
        return sellGUI;
    }

    public AchievementUtil getAchievementUtil() {
        return achievementUtil;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public QuestGUI getQuestGUI() {
        return questGUI;
    }

    public ThemeGUI getThemeGUI() {
        return themeGUI;
    }

    public MerchantListener getMerchantListener() {
        return merchantListener;
    }

    public CombatListener getCombatListener() {
        return combatListener;
    }
}
