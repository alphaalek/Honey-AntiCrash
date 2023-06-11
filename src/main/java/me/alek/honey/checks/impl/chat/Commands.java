package me.alek.honey.checks.impl.chat;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.wrappers.play.client.WrappedPlayInChat;
import me.alek.honey.services.ViolationService;
import org.bukkit.craftbukkit.libs.org.eclipse.aether.util.StringUtils;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class Commands extends AsyncPacketAdapter<WrappedPlayInChat> implements Check {

    private static final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    private List<String> prohibitedCommands;

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInChat> packetContainer) {
        String message = packetContainer.getPacket().getMessage();
        if (!message.startsWith("/")) {
            return;
        }
        for (String prohibitedCommand : prohibitedCommands) {
            if (message.equals(prohibitedCommand)) {
                VIOLATION_SERVICE.violate(player, packetContainer.getPacket(), this);
                packetContainer.cancel();
                return;
            }
        }
    }


    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CHAT);
    }

    @Override
    public String getName() {
        return "Commands";
    }

    @Override
    public void load() {
        prohibitedCommands = HoneyPlugin.get().getConfigurationOptions().getProhibitedCommands();
    }
}
