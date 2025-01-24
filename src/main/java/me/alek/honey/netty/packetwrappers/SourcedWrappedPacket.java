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

    default <T> T decodeExternally(Callable<T> callable) {
        Callback exCallback = new Callback();
        exCallback.addAction(Callback.Action.<Throwable>from(ex -> {
            if (HoneyPlugin.get().getLogger().isDebug()) {
                Bukkit.getLogger().log(Level.WARNING,
                        "Error occurred while decoding packet " + getContainer().getType() + " (id " + getContainer().getType().getMinecraftID().get()
                                + ") sent by " + getPlayer().name, ex);
            }

            getPlayer().failPacket(ex, getContainer());
        }));
        return getPlayer().executeExternally(callable, exCallback);
    }

    default NetProtocol getProtocol() {
        return getContainer().getProtocol();
    }

    PacketContainer<WP> getContainer();

    HoneyPlayer getPlayer();
}
