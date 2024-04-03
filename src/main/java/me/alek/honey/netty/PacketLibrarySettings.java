package me.alek.honey.netty;

import me.alek.honey.netty.utility.NetProtocol;
import org.bukkit.plugin.Plugin;

public class PacketLibrarySettings {

    private NetProtocol fallbackProtocol;
    private final Plugin plugin;
    private final String filterName;
    private final String connectorName;
    private final String channelInitName;
    private final String decoderName;
    private final String encoderName;
    private final String debuggerName;

    public PacketLibrarySettings(
            Plugin plugin,
            String filterName,
            String connectorName,
            String channelInitName,
            String decoderName,
            String encoderName,
            String debuggerName
    ) {
        this.plugin = plugin;
        this.filterName = filterName;
        this.connectorName = connectorName;
        this.channelInitName = channelInitName;
        this.decoderName = decoderName;
        this.encoderName = encoderName;
        this.debuggerName = debuggerName;
    }

    public String getFilterName() {
        return filterName;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public String getChannelInitName() {
        return channelInitName;
    }

    public String getDecoderName(){
        return decoderName;
    }

    public String getEncoderName() {
        return encoderName;
    }

    public String getDebuggerName() {
        return debuggerName;
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
