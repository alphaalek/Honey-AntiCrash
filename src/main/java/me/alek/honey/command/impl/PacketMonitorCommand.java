package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.monitor.MonitorFlag;
import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

public class PacketMonitorCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender, String[] args) {

        try {
            List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> flags = MonitorFlag.Flag.getMonitorFlags(args);
            Bukkit.broadcastMessage("Flags: " + flags);

            if (flags.isEmpty()) {
                sender.sendMessage(CommandManager.PREFIX + " §7You have to choose at least one flag for the packet monitor.");

                sender.sendMessage(CommandManager.PREFIX + " §7You can use the following flags:");
                for (MonitorFlag.Flag flag : MonitorFlag.Flag.values()) {
                    sender.sendMessage("§e" + flag.getUsage());
                }
                return;
            }

            HoneyPlugin.get().getMonitorManager().setupPacketMonitor(sender, flags);

        } catch (WrongMonitorFlagSyntaxException ex) {
            sender.sendMessage(CommandManager.PREFIX + " §cWrong syntax of monitor \"flag\" " + ex.getFlag() + ": " + ex.getMessage());
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
