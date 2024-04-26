package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.monitor.MonitorFlag;
import me.alek.honey.monitor.MonitorFlagParser;
import me.alek.honey.monitor.Monitorer;
import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

public class PacketMonitorCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender, String[] args) {

        try {
            Array<Object> parsed = MonitorFlagParser.getMonitorFlags(args);
            List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> flags = parsed.stream().filter(p -> p instanceof MonitorFlag<?>).map(p -> (MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>) p).collect(Collectors.toList());

            if (flags.isEmpty()) {
                sender.sendMessage(CommandManager.PREFIX + " §7You have to choose at least one flag for the packet monitor. Remember you can use the same flag multiple times.");

                sender.sendMessage(CommandManager.PREFIX + " §7You can use the following flags:");
                for (MonitorFlag.Flag flag : MonitorFlag.Flag.values()) {
                    sender.sendMessage("  §8" + flag.getUsage());
                }
                HoneyPlugin.get().getMonitorManager().removeIfHasMonitor(sender, false);
                return;
            }
            HoneyPlugin.get().getMonitorManager().setupPacketMonitor(sender, flags);
            sender.sendMessage(CommandManager.PREFIX + " §7Using the following flags: ");
            for (MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag : flags) {
                sender.sendMessage("  §8--" + Optional.ofNullable(MonitorFlagParser.getFlag(flag)).map(MonitorFlag.Flag::getName).orElse("Unknown") + " " + flag.getSpecified());
            }

        } catch (WrongMonitorFlagSyntaxException ex) {
            sender.sendMessage(CommandManager.PREFIX + " §cWrong syntax of monitor flag \"" + ex.getFlag() + "\": " + ex.getMessage());
        }
    }

    @Override
    public String getName() {
        return "packetmonitor";
    }

    @Override
    public String getUsage() {
        return "/honey packetmonitor";
    }

    @Override
    public String getDescription() {
        return "Advanced packet monitor with customizable flags.";
    }

    @Override
    public String getPermission() {
        return "honey.command.packetmonitor";
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
