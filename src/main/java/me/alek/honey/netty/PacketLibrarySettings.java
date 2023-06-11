package me.alek.honey.netty;

import me.alek.honey.netty.utility.protocol.Protocol;
import org.bukkit.plugin.java.JavaPlugin;

public class PacketLibrarySettings {

    private boolean useLateInjection = false;
    private Protocol fallbackProtocol;
    private final JavaPlugin plugin;
    private final String handlerName;

    public PacketLibrarySettings(
            JavaPlugin plugin,
            String handlerName
    ) {
        this.plugin = plugin;
        this.handlerName = handlerName;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setUseLateInjection(boolean value) {
        this.useLateInjection = value;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public boolean useLateInjection() {
        return useLateInjection;
    }

    public void setFallbackProtocol(Protocol value) {
        this.fallbackProtocol = value;
    }

    public Protocol getFallbackProtocol() {
        return fallbackProtocol;
    }
}
