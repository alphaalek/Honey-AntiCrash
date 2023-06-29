package me.alek.honey.netty;

import me.alek.honey.netty.utility.protocol.Protocol;
import org.bukkit.plugin.java.JavaPlugin;

public class PacketLibrarySettings {

    private boolean useLateInjection;
    private Protocol fallbackProtocol;
    private final JavaPlugin plugin;
    private final String handlerName;
    private final String decoderName;

    public PacketLibrarySettings(
            JavaPlugin plugin,
            boolean useLateInjection,
            String handlerName,
            String decoderName
    ) {
        this.useLateInjection = useLateInjection;
        this.plugin = plugin;
        this.handlerName = handlerName;
        this.decoderName = decoderName;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public String getDecoderName() {
        return decoderName;
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
