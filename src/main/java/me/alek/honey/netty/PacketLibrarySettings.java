package me.alek.honey.netty;

import me.alek.honey.netty.utility.NetProtocol;
import org.bukkit.plugin.Plugin;

public class PacketLibrarySettings {

    private NetProtocol fallbackProtocol;
    private final Plugin plugin;
    private final String handlerName;
    private final String decoderName;
    private final String connectionName;

    public PacketLibrarySettings(
            Plugin plugin,
            String handlerName,
            String decoderName,
            String connectorName
    ) {
        this.plugin = plugin;
        this.handlerName = handlerName;
        this.decoderName = decoderName;
        this.connectionName = connectorName;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public String getDecoderName() {
        return decoderName;
    }

    public String getConnectorName() {
        return connectionName;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setFallbackProtocol(NetProtocol value) {
        this.fallbackProtocol = value;
    }

    public NetProtocol getFallbackProtocol() {
        return fallbackProtocol;
    }
}
