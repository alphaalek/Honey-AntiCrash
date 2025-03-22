package me.alek.honey.monitor;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;

import java.util.*;

public class PacketMonitorer<WP extends SourcedWrappedPacket<WP>> implements IndependentMonitorer<PacketContainer<WP>> {

    private final FlagRunner.Env<WP> flags;
    private final Map<SendProperty, List<MonitorFlag<PacketContainer<WP>>>> linkSendMap = new HashMap<>();

    public PacketMonitorer(FlagRunner.Env<WP> flags) {
        this.flags = flags;

        for (MonitorFlag<PacketContainer<WP>> flag : flags.getAllFlags()) {
            MonitorFlag.Flag litFlag = MonitorFlagParser.getFlag(flag);
            if (litFlag != null) {
                for (SendProperty property : litFlag.getSendProperties()) {
                    this.linkSendMap.computeIfAbsent(property, __ -> new ArrayList<>()).add(flag);
                }
            }
        }
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
    public List<SendProperty> getSendProperties() {
        return new ArrayList<>(this.linkSendMap.keySet());
    }
}
