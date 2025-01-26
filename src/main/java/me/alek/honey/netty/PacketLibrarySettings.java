package me.alek.honey.netty;

import org.bukkit.plugin.Plugin;

public class PacketLibrarySettings {

    private final Plugin plugin;
    private final String filterName;
    private final String connectorName;
    private final String channelInitName;
    private final String inStreamName;
    private final String outStreamName;
    private final String debuggerName;

    public PacketLibrarySettings(
            Plugin plugin,
            String filterName,
            String connectorName,
            String channelInitName,
            String inStreamName,
            String outStreamName,
            String debuggerName) {
        this.plugin = plugin;
        this.filterName = filterName;
        this.connectorName = connectorName;
        this.channelInitName = channelInitName;
        this.inStreamName = inStreamName;
        this.outStreamName = outStreamName;
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

    public String getInStreamName() {
        return inStreamName;
    }

    public String getOutStreamName() {
        return outStreamName;
    }

    public String getDebuggerName() {
        return debuggerName;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
