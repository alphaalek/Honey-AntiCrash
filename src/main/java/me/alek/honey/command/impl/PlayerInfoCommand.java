package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.lang.Lang;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class PlayerInfoCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Lang.getFormatted("unknown.player"));
        }
        Optional<HoneyPlayer> target = getPlayerBy(args[0]);
        if (!target.isPresent()) {
            sender.sendMessage(Lang.getFormatted("unknown.player"));
            return;
        }

        HoneyPlayer player = target.get();
        sender.sendMessage("§6Honey §7Showing info for player §6" + player.name);
        sender.sendMessage("§8* §7Using client §8" + player.clientInfo.getBrand());
        sender.sendMessage("§8* §7Using version §8" + player.clientInfo.getMajorProtocol().getName() + ", v" + player.clientInfo.getProtocolVersion() + " (server: " + NetProtocol.getProtocol().getName() + ")");

        if (player.bypassEverything()) {
            sender.sendMessage("§cThis player bypasses everything!");
        } else if (player.bypassServerThreadTracking()) {
            sender.sendMessage("§cThis player bypasses server thread tracking!");
        }
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getUsage() {
        return "/honey info <player>";
    }

    @Override
    public String getDescription() {
        return Lang.get("command.desc.info");
    }

    @Override
    public String getPermission() {
        return "honey.command.info";
    }

    @Override
    public boolean isExecutableByConsole() {
        return true;
    }

    @Override
    public int getCooldown() {
        return 1;
    }
}
