package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;

public interface WrappedPacket<WP extends WrappedPacket<WP>> {

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

    @SuppressWarnings("unchecked")
    default WP get() {
        return (WP) this;
    }

    default boolean isByteBased() {
        return false;
    }

    default boolean isObjectBased() {
        return false;
    }

    HoneyPlayer getPlayer();

    PacketContainer<WP> getContainer();

    PacketType getType();

    Map<String, Object> getProperties();
}


