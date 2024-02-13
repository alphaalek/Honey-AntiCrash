package me.alek.honey.monitor;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PacketMonitorer<WP extends WrappedPacket<WP>> implements IndependentMonitorer<PacketContainer<? extends WrappedPacket<WP>>> {

    private final List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> flags;
    private final MonitorSendAt sendAt;

    public PacketMonitorer(List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> flags) {
        this.flags = flags;

        boolean hasToBeAfterServerHandle = false;
        for (MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag : flags) {
            hasToBeAfterServerHandle = Optional.ofNullable(
                    MonitorFlag.Flag.getFlag(flag)).map(MonitorFlag.Flag::hasToBeAfterServerHandle).orElse(false);
            if (hasToBeAfterServerHandle) break;
        }
        sendAt = hasToBeAfterServerHandle ? MonitorSendAt.AFTER_SERVER_HANDLE : MonitorSendAt.AFTER_HONEY_HANDLE;
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
            dataMessage.append("§8, §7").append(d);
        }
        return "§8[§6§l!§8] §e" + input.getPlayer().getName() + " §7" + input.getType() + dataMessage;
    }

    @Override
    public MonitorSendAt getSendAt() {
        return sendAt;
    }
}
