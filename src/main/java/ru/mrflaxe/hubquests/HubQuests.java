package ru.mrflaxe.hubquests;

import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import ru.mrflaxe.hubquests.commands.CommandQuest;
import ru.mrflaxe.hubquests.database.DatabaseManager;
import ru.mrflaxe.hubquests.database.model.ProfileModel;
import ru.mrflaxe.hubquests.database.model.SecretModel;
import ru.mrflaxe.hubquests.gui.MenuConfiguration;
import ru.mrflaxe.hubquests.gui.MenuProvider;
import ru.mrflaxe.hubquests.listeners.CustomEventsListener;
import ru.mrflaxe.hubquests.listeners.InventoryListener;
import ru.mrflaxe.hubquests.listeners.PlayerListener;
import ru.mrflaxe.hubquests.protocol.PacketListener;
import ru.mrflaxe.hubquests.protocol.PacketManager;
import ru.mrflaxe.hubquests.session.SessionContainer;
import ru.soknight.lib.configuration.Configuration;
import ru.soknight.lib.configuration.Messages;
import ru.soknight.lib.database.Database;
import ru.soknight.peconomy.PEconomy;
import ru.soknight.peconomy.api.PEconomyAPI;

public class HubQuests extends JavaPlugin {

    private Configuration config;
    private Messages messages;
    private MenuConfiguration menuConfig;
    private MenuProvider menuProvider;
    private DatabaseManager databaseManager;
    private PacketManager packetManager;
    private SessionContainer sessionContainer;
    
    private PEconomyAPI api;
    
    @Override
    public void onEnable() {
        refreshConfigs();
        
        api = PEconomy.getAPI();
        
        if(api == null) {
            // If it's null then PEconomy is not initialized
            getLogger().severe("PEconomy is not initialized.");
            
            // We need PEconomy and if it's not initialized we just disable this plugin
            Bukkit.getPluginManager().disablePlugin(this);
            // And return this method
            return;
        }
        
        // database initialization
        try {
            Database database = new Database(this, config)
                    .createTable(ProfileModel.class)
                    .createTable(SecretModel.class)
                    .complete();
            this.databaseManager = new DatabaseManager(database, this);
        } catch (SQLException ex) {
            getLogger().severe("Failed to establish a database connection: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize the database!");
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        refreshDatabase();
        
        this.packetManager = new PacketManager(databaseManager, messages, config);
        this.sessionContainer = new SessionContainer(config, messages, databaseManager, packetManager, this);
        this.menuProvider = new MenuProvider(messages, menuConfig, config, databaseManager, sessionContainer, this);
        
        registerCommands();
        registerListeners();
        
        
    }
    
    @Override
    public void onDisable() {
        if(databaseManager != null)
        databaseManager.shutdown();
        
        if(sessionContainer != null)
        sessionContainer.closeAllSessions(packetManager);
    }
    
    public void refreshConfigs() {
        if(config == null) this.config = new Configuration(this, "config.yml");
        config.refresh();
        
        if(messages == null ) this.messages = new Messages(this, "messages.yml");
        messages.refresh();
        
        if(menuConfig == null) this.menuConfig = new MenuConfiguration(this);
        menuConfig.refresh();
    }
    
    private void registerCommands() {
        new CommandQuest(messages, menuProvider, this);
    }
    
    private void registerListeners() {
        new PlayerListener(databaseManager, packetManager, sessionContainer, this).register();
        new InventoryListener(messages, config, menuConfig, menuProvider, databaseManager, sessionContainer, packetManager, this).register(this);
        new CustomEventsListener(messages, config, this).register();
        
        new PacketListener(config, databaseManager, packetManager, menuProvider, sessionContainer, api, this);
    }
    
    private void refreshDatabase() {
        Bukkit.getOnlinePlayers().parallelStream()
            .filter(player -> !databaseManager.hasProfile(player.getName()).join())
            .forEach(player -> databaseManager.saveProfile(new ProfileModel(player.getName())));
    }
}
