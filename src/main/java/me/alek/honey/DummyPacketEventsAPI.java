package me.alek.honey;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.netty.NettyManager;
import io.github.retrooper.packetevents.manager.protocol.ProtocolManagerImpl;
import io.github.retrooper.packetevents.manager.server.ServerManagerImpl;
import io.github.retrooper.packetevents.netty.NettyManagerImpl;
import org.bukkit.plugin.Plugin;

public class DummyPacketEventsAPI extends PacketEventsAPI<Plugin> {

    private final ProtocolManager protocolManager = new ProtocolManagerImpl();
    private final ServerManager serverManager = new ServerManagerImpl();
    private final NettyManager nettyManager = new NettyManagerImpl();
    private boolean loaded;
    private boolean initialized;
    private boolean terminated = false;

    @Override
    public void load() {
        this.loaded = true;
    }

    @Override
    public boolean isLoaded() {
        return this.loaded;
    }

    @Override
    public void init() {
        this.initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public void terminate() {
        this.terminated = true;
    }

    @Override
    public boolean isTerminated() {
        return this.terminated;
    }

    @Override
    public Plugin getPlugin() {
        return HoneyPlugin.get().getPlugin();
    }

    @Override
    public ServerManager getServerManager() {
        return this.serverManager;
    }

    @Override
    public ProtocolManager getProtocolManager() {
        return this.protocolManager;
    }

    @Override
    public NettyManager getNettyManager() {
        return this.nettyManager;
    }

    @Override
    public PlayerManager getPlayerManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelInjector getInjector() {
        throw new UnsupportedOperationException();
    }
}
