package com.italiarevenge.iRShop.command;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.gui.SellGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellGuiCommand implements CommandExecutor {

    private final MessageManager msg;

    public SellGuiCommand(IRShop plugin) {
        this.msg = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.get("general.player-only"));
            return true;
        }
        if (!player.hasPermission("irshop.use")) {
            player.sendMessage(msg.get("general.no-permission"));
            return true;
        }
        new SellGui(player).open();
        return true;
    }
}
