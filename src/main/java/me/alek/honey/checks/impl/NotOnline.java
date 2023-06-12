package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.services.NotificationService;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Supplier;

public class NotOnline extends FuzzyPacketAdapter implements Check {

    private static final NotificationService NOTIFICATION_SERVICE = HoneyPlugin.get().getNotificationService();

    @Override
    public void onPacketReceive(Player player, PacketContainer packetContainer) {
        if (!player.isOnline()) {
            packetContainer.cancel();
            NMSUtils.getChannel(player).close();
            NOTIFICATION_SERVICE.notify("some reason");
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, Protocol.getProtocol());
    }

    @Override
    public String getName() {
        return "Not Online";
    }
}
