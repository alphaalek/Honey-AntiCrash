package me.alek.honey.monitor;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PacketMonitorer<WP extends WrappedPacket<WP>> implements IndependentMonitorer<PacketContainer<? extends WrappedPacket<WP>>> {

    private final FlagRunner.RuntimeFlags flags;
    private final MonitorSendAt sendAt;

    public PacketMonitorer(FlagRunner.RuntimeFlags flags) {
        this.flags = flags;

        boolean hasToBeAfterServerHandle = false;
        for (MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag : flags.getFlags()) {
            hasToBeAfterServerHandle = Optional.ofNullable(
                    MonitorFlagParser.getFlag(flag)).map(MonitorFlag.Flag::hasToBeAfterServerHandle).orElse(false);

            if (hasToBeAfterServerHandle) {
                break;
            }
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
        List<FlagRunner.AppendedData> data = new ArrayList<>();
        if (!flags.process(input, data)) {
            return null;
        }

        StringBuilder dataMessage = new StringBuilder();
        for (FlagRunner.AppendedData d : data) {
            if (d.shouldAppend()) {
                dataMessage.append("§8, §7").append(d.getData());
            }
        }
        return "§8[§6§l!§8] §e" + input.getPlayerName() + " §7" + input.getType() + dataMessage;
    }

    @Override
    public MonitorSendAt getSendAt() {
        return sendAt;
    }
}
