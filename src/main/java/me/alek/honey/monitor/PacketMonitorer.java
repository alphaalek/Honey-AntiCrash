package me.alek.honey.monitor;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PacketMonitorer<WP extends WrappedPacket<WP>> implements IndependentMonitorer<PacketContainer<WP>> {

    private final FlagRunner.Env<WP> flags;
    private final MonitorSendAt sendAt;

    public PacketMonitorer(FlagRunner.Env<WP> flags) {
        this.flags = flags;

        boolean hasToBeAfterServerHandle = false;
        for (MonitorFlag<PacketContainer<WP>> flag : flags.getAllFlags()) {
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
    public String getMessage(PacketContainer<WP> input) {
        if (!this.flags.test(input)) {
            return null;
        }

        List<String> data = new ArrayList<>();
        this.flags.append(input, data);

        StringBuilder dataMessage = new StringBuilder();
        for (String d : data) {
            dataMessage.append("§8, §7").append(d);
        }
        return "§8[§6§l!§8] §e" + input.getPlayerName() + " §7" + input.getType() + dataMessage;
    }

    @Override
    public MonitorSendAt getSendAt() {
        return sendAt;
    }
}
