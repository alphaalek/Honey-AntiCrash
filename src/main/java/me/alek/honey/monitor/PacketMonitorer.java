package me.alek.honey.monitor;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.util.ArrayList;
import java.util.List;

public class PacketMonitorer<WP extends WrappedPacket<WP>> implements IndependentMonitorer<PacketContainer<? extends WrappedPacket<WP>>> {

    private final List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> flags;

    public PacketMonitorer(List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> flags) {
        this.flags = flags;
    }

    @Override
    public MonitorType getType() {
        return MonitorType.PACKETS;
    }

    @Override
    public String getName() {
        return "packet monitor";
    }

    @Override
    public String getMessage(PacketContainer<? extends WrappedPacket<WP>> input) {
        List<String> data = new ArrayList<>();
        for (MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag : flags) {
            if (!flag.test(input)) {
                return null;
            }
            flag.append(input, data);
        }
        StringBuilder dataMessage = new StringBuilder();
        for (String d : data) {
            dataMessage.append(" ").append(d);
        }
        return "§8[§6§l!§8] §e" + input.getPlayer().getName() + " §7" + input.getType() + dataMessage;
    }
}
