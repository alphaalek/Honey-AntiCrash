package me.alek.honey.netty;

import me.alek.honey.netty.utility.NetProtocol;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class PacketLibrarySettings {

    private boolean useLateInjection;
    private NetProtocol fallbackProtocol;
    private final Plugin plugin;
    private final String handlerName;
    private final String decoderName;

    public PacketLibrarySettings(
            Plugin plugin,
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

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean useLateInjection() {
        return useLateInjection;
    }

    public void setFallbackProtocol(NetProtocol value) {
        this.fallbackProtocol = value;
    }

    public NetProtocol getFallbackProtocol() {
        return fallbackProtocol;
    }
}
