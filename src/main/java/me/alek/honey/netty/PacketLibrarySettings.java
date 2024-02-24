package me.alek.honey.netty;

import me.alek.honey.netty.utility.NetProtocol;
import org.bukkit.plugin.Plugin;

public class PacketLibrarySettings {

    private NetProtocol fallbackProtocol;
    private final Plugin plugin;
    private final String handlerName;
    private final String filterName;
    private final String connectorName;

    public PacketLibrarySettings(
            Plugin plugin,
            String handlerName,
            String filterName,
            String connectorName
    ) {
        this.plugin = plugin;
        this.handlerName = handlerName;
        this.filterName = filterName;
        this.connectorName = connectorName;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public String getFilterName() {
        return filterName;
    }

    public String getConnectorName() {
        return connectorName;
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
