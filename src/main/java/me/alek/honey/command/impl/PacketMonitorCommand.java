package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.lang.Lang;
import me.alek.honey.monitor.Flags;
import me.alek.honey.monitor.MonitorFlag;
import me.alek.honey.monitor.MonitorFlagParser;
import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class PacketMonitorCommand implements CommandImpl {

    @Override
    public void perform(CommandSender sender, String[] args) {

        try {
            Flags.RuntimeFlags flags = MonitorFlagParser.getMonitorFlags(args);

            if (flags.isEmpty()) {
                sender.sendMessage(Lang.getFormatted("packetmonitor.min.flag.1"));
                sender.sendMessage(Lang.getFormatted("packetmonitor.min.flag.2"));
                for (MonitorFlag.Flag flag : MonitorFlag.Flag.values()) {
                    sender.sendMessage("  §8" + flag.getUsage());
                }
                HoneyPlugin.get().getMonitorManager().removeIfHasMonitor(sender, false);
                return;
            }
            HoneyPlugin.get().getMonitorManager().setupPacketMonitor(sender, flags);
            sender.sendMessage(Lang.getFormatted("packetmonitor.using.flags"));
            for (MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag : flags.getFlags()) {
                String specified = flag.getSpecified();
                sender.sendMessage("  §8--" + Optional.ofNullable(MonitorFlagParser.getFlag(flag)).map(MonitorFlag.Flag::getName).orElse("Unknown") + (specified.isEmpty() ? "" : ": " + specified));
            }

        } catch (WrongMonitorFlagSyntaxException ex) {
            sender.sendMessage(Lang.getFormatted("packetmonitor.wrong.flag.syntax", ex.getType(), ex.getMessage()));
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
        return Lang.get("command.desc.packetmonitor");
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
