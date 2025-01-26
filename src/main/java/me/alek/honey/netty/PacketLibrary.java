package me.alek.honey.netty;

import me.alek.honey.netty.bukkit.BukkitEventInternal;
import me.alek.honey.netty.injector.ServerPacketManager;
import me.alek.honey.netty.injector.SpigotPipelineInjector;
import me.alek.honey.netty.injector.SpigotServerInjector;
import me.alek.honey.netty.packet.*;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.processor.PacketProcessor;
import me.alek.honey.netty.processor.TypeProcessors;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PacketLibrary {

    private final PacketLibrarySettings settings;
    private final PacketProcessor packetProcessor;
    private final SpigotPipelineInjector pipelineInjector;
    private final SpigotServerInjector serverInjector;
    private final ServerPacketManager packetManager;
    private boolean enabled = true;

    private static PacketLibrary INSTANCE;

    public static PacketLibrary set(PacketLibrarySettings settings) {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        return new PacketLibrary(settings);
    }

    public static PacketLibrary get() {
        return INSTANCE;
    }

    private PacketLibrary(PacketLibrarySettings settings) {
        INSTANCE = this;
        this.settings = settings;
        Bukkit.getServer().getPluginManager().registerEvents(new BukkitEventInternal(), settings.getPlugin());

        PacketTypes.load();
        WrappedPacketFactoryCollection.loadWrappers();

        this.packetProcessor = new InternalPacketProcessor();
        this.packetManager = new ServerPacketManager();

        this.pipelineInjector = new SpigotPipelineInjector();
        this.serverInjector = new SpigotServerInjector();
        if (serverInjector.isServerBound()) {
            this.serverInjector.inject();
        } else {
            // late bind, so we inject a bit later
            Bukkit.getScheduler().scheduleSyncDelayedTask(settings.getPlugin(), this.serverInjector::inject);
        }

        PostActions.setup();

        TypeProcessors.loadImpl();
    }

    public SpigotServerInjector getServerInjector() {
        return this.serverInjector;
    }

    public SpigotPipelineInjector getPipelineInjector() {
        return this.pipelineInjector;
    }

    public ServerPacketManager getPacketManager() {
        return this.packetManager;
    }

    public String getFilterName() {
        return this.settings.getFilterName();
    }

    public String getConnectorName() {
        return this.settings.getConnectorName();
    }

    public String getChannelInitName() {
        return this.settings.getChannelInitName();
    }

    public String getInStreamName() {
        return this.settings.getInStreamName();
    }

    public String getOutStreamName() {
        return this.settings.getOutStreamName();
    }

    public String getDebuggerName() {
        return this.settings.getDebuggerName();
    }

    public PacketLibrarySettings getSettings() {
        return this.settings;
    }

    public PacketProcessor getPacketProcessor() {
        return this.packetProcessor;
    }

    public <WP extends SourcedWrappedPacket<WP>> void addListeners(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state, ListenerPriority priority, List<PacketType> types) {
        this.packetProcessor.addListener(listener, state, priority, types);
    }

    public <WP extends SourcedWrappedPacket<WP>> void addListeners(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state, ListenerPriority priority, PacketType... types) {
        addListeners(listener, state, priority, Arrays.asList(types));
    }

    public <WP extends SourcedWrappedPacket<WP>> void removeListener(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state) {
        removeListeners(listener, state, PacketTypes.getTypesAvailable().stream().collect(Collectors.toList()));
    }

    public <WP extends SourcedWrappedPacket<WP>> void removeListeners(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state, List<PacketType> types) {
        this.packetProcessor.removeListener(listener, state, types);
    }

    public <WP extends SourcedWrappedPacket<WP>> boolean isRegistered(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state) {
        return this.packetProcessor.isRegistered(listener, state);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void disable() {
        this.enabled = false;
        this.serverInjector.eject();
    }
}
