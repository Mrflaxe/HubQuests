package ru.mrflaxe.hubquests.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import ru.mrflaxe.hubquests.HubQuests;
import ru.mrflaxe.hubquests.gui.MenuProvider;
import ru.soknight.lib.argument.CommandArguments;
import ru.soknight.lib.command.preset.standalone.PlayerOnlyCommand;
import ru.soknight.lib.configuration.Messages;

public class CommandQuest extends PlayerOnlyCommand {

    private final Messages messages;
    private final MenuProvider menuProvider;
    private final HubQuests plugin;
    
    public CommandQuest(Messages messages, MenuProvider menuProvider, HubQuests plugin) {
        super("quest", "hubquests.command.quest", messages);
        
        this.messages = messages;
        this.menuProvider = menuProvider;
        this.plugin = plugin;
        
        super.register(plugin);
    }

    @Override
    protected void executeCommand(CommandSender sender, CommandArguments args) {
        Player player = (Player) sender;
        
        if(args.size() > 0) {
            String command = args.get(0);
            
            if(command.equals("customizer")) {
                if(!sender.hasPermission("hubquests.command.quest.customizer")) {
                    messages.getAndSend(sender, "error.no-permissions");
                    return;
                }
                
                int firstEmpty = player.getInventory().firstEmpty();
                if(firstEmpty == -1) {
                    messages.getAndSend(sender, "error.full-inventory");
                    return;
                }
                
                ItemStack item = new ItemStack(Material.BLAZE_ROD);
                ItemMeta meta = item.getItemMeta();
                
                NamespacedKey key = new NamespacedKey(plugin, "type");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "admin");
                
                String name = messages.getColoredString("customizer.name");
                List<String> lore = messages.getColoredList("customizer.lore");
                
                meta.setDisplayName(name);
                meta.setLore(lore);
                
                item.setItemMeta(meta);
                
                player.getInventory().setItem(firstEmpty, item);
                messages.getAndSend(player, "command.admin.success");
                return;
            }
            
            if(command.equals("refresh")) {
                if(!sender.hasPermission("hubquests.command.quest.refresh")) {
                    messages.getAndSend(sender, "error.no-permissions");
                    return;
                }
                
                plugin.refreshConfigs();
                messages.getAndSend(sender, "command.refresh");
                return;
            }
        }
        
        menuProvider.openMenu(player);
        
        return;
    }

    @Override
    protected List<String> executeTabCompletion(CommandSender sender, CommandArguments args) {
        if(!sender.hasPermission("command.quest.customizer")) return null;
        
        List<String> completion = new ArrayList<>();
        completion.add("customizer");
        completion.add("refresh");
        
        return completion.stream()
                .filter(s -> s.startsWith(args.get(0)))
                .collect(Collectors.toList());
    }
    
}
