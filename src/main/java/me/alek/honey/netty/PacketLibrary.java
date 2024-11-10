package me.alek.honey.netty;

import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.bukkit.BukkitEventInternal;
import me.alek.honey.netty.injector.ServerPacketManager;
import me.alek.honey.netty.injector.SpigotPipelineInjector;
import me.alek.honey.netty.injector.SpigotServerInjector;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.WrappedPacketFactoryCollection;
import me.alek.honey.netty.packet.PostActions;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.InternalPacketProcessor;
import me.alek.honey.netty.processor.Processors;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
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
        if (INSTANCE != null) return INSTANCE;

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

        packetProcessor = new InternalPacketProcessor();
        packetManager = new ServerPacketManager();

        pipelineInjector = new SpigotPipelineInjector();

        serverInjector = new SpigotServerInjector();
        if (serverInjector.isServerBound()) {
            serverInjector.inject();
        } else {
            // late bind, so we inject a bit later
            Bukkit.getScheduler().scheduleSyncDelayedTask(settings.getPlugin(), serverInjector::inject);
        }

        PostActions.setup();

        Processors.dummyLoad();
    }

    public SpigotServerInjector getServerInjector() {
        return serverInjector;
    }

    public SpigotPipelineInjector getPipelineInjector() {
        return pipelineInjector;
    }

    public ServerPacketManager getPacketManager() {
        return packetManager;
    }

    public String getFilterName() {
        return settings.getFilterName();
    }

    public String getConnectorName() {
        return settings.getConnectorName();
    }

    public String getChannelInitName() {
        return settings.getChannelInitName();
    }

    public String getDecoderName() {
        return settings.getDecoderName();
    }

    public String getEncoderName() {
        return settings.getEncoderName();
    }

    public String getDebuggerName() {
        return settings.getDebuggerName();
    }

    public PacketLibrarySettings getSettings() {
        return settings;
    }

    public PacketProcessor getPacketProcessor() {
        return packetProcessor;
    }

    public <WP extends WrappedPacket<WP>> void addListeners(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state, ListenerPriority priority, List<PacketType> types) {
        packetProcessor.addListener(listener, state, priority, types);
    }

    public <WP extends WrappedPacket<WP>> void addListeners(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state, ListenerPriority priority, PacketType... types) {
        addListeners(listener, state, priority, Arrays.asList(types));
    }

    public <WP extends WrappedPacket<WP>> void removeListener(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state) {
        removeListeners(listener, state, PacketTypes.getTypesAvailable().stream().collect(Collectors.toList()));
    }

    public <WP extends WrappedPacket<WP>> void removeListeners(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state, List<PacketType> types) {
        packetProcessor.removeListener(listener, state, types);
    }

    public <WP extends WrappedPacket<WP>> boolean isRegistered(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state) {
        return packetProcessor.isRegistered(listener, state);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        enabled = false;
        serverInjector.eject();
    }
}
