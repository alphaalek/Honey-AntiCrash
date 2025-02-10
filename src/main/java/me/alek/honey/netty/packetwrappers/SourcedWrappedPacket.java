package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.concurrent.Callable;
import java.util.logging.Level;

public interface SourcedWrappedPacket<WP extends SourcedWrappedPacket<WP>> extends WrappedPacket<WP> {

    default NetProtocol getProtocol() {
        return getContainer().getProtocol();
    }

    default NetProtocol getProtocolCreated() {
        return getContainer().getProtocolCreated();
    }

    boolean isAbstract();

    PacketContainer.PipelineState getState();

    PacketContainer<WP> getContainer();

    HoneyPlayer getPlayer();
}
