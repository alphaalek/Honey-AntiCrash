package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PipelineDebugCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length == 0 && !(sender instanceof Player)) {
            sender.sendMessage("§cUsage: /honey pipeline <player>");
        } else if (args.length == 0) {
            Player player = (Player) sender;
            HoneyPlayer pl = HoneyPlayer.getPlayer(player.getUniqueId());
            sender.sendMessage(String.valueOf(pl.channel.pipeline().names()));
        } else {
            String targetStr = args[0];
            Player player = Bukkit.getPlayer(targetStr);
            if (player == null) {
                sender.sendMessage(Lang.getFormatted("unknown.player"));
            } else {
                HoneyPlayer pl = HoneyPlayer.getPlayer(player.getUniqueId());
                sender.sendMessage(String.valueOf(pl.channel.pipeline().names()));
            }
        }
    }

    @Override
    public String getName() {
        return "pipeline";
    }

    @Override
    public String getUsage() {
        return "/honey pipeline <player>";
    }

    @Override
    public String getDescription() {
        return Lang.get("command.desc.pipelinedebug");
    }

    @Override
    public String getPermission() {
        return "honey.command.pipelinedebug";
    }

    @Override
    public boolean isExecutableByConsole() {
        return true;
    }
}
