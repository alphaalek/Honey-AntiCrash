package me.alek.honey.netty;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.bukkit.BukkitEventInternal;
import me.alek.honey.netty.injector.ChannelManager;
import me.alek.honey.netty.injector.SpigotPipelineInjector;
import me.alek.honey.netty.injector.SpigotServerInjector;
import me.alek.honey.netty.packet.*;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.processor.InternalPacketProcessor;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.processor.PacketProcessor;
import me.alek.honey.netty.processor.TypeProcessors;
import me.alek.honey.netty.tag.nbt.TagType;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PacketLibrary {

    private final PacketProcessor packetProcessor;
    private final SpigotPipelineInjector pipelineInjector;
    private final SpigotServerInjector serverInjector;
    private final ChannelManager channelManager;
    private boolean enabled = true;

    private static PacketLibrary INSTANCE;

    public static void load() {
        INSTANCE = new PacketLibrary();
    }

    public static PacketLibrary get() {
        return INSTANCE;
    }

    private PacketLibrary() {
        INSTANCE = this;
        Bukkit.getServer().getPluginManager().registerEvents(new BukkitEventInternal(), HoneyPlugin.get().getPlugin());

        PacketTypes.load();
        WrappedPacketFactoryCollection.loadWrappers();

        this.packetProcessor = new InternalPacketProcessor();
        this.channelManager = new ChannelManager();

        this.pipelineInjector = new SpigotPipelineInjector();
        this.serverInjector = new SpigotServerInjector();
        if (serverInjector.hasAnyListeningChannel()) {
            this.serverInjector.inject();
        } else {
            // late bind, so we inject a bit later
            Bukkit.getScheduler().scheduleSyncDelayedTask(HoneyPlugin.get().getPlugin(), this.serverInjector::inject);
        }

        TagType.load();

        PostActions.setup();
        TypeProcessors.loadImpl();
    }

    public SpigotServerInjector getServerInjector() {
        return this.serverInjector;
    }

    public SpigotPipelineInjector getPipelineInjector() {
        return this.pipelineInjector;
    }

    public ChannelManager getChannelManager() {
        return this.channelManager;
    }

    public PacketProcessor getPacketProcessor() {
        return this.packetProcessor;
    }

    public <WP extends SourcedWrappedPacket<WP>> void addListener(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state, ListenerPriority priority, List<PacketType> types) {
        this.packetProcessor.addListener(listener, state, priority, types);
    }

    public <WP extends SourcedWrappedPacket<WP>> void addListener(PacketListenerAdapter<WP> listener, PacketContainer.PipelineState state, ListenerPriority priority, PacketType... types) {
        addListener(listener, state, priority, Arrays.asList(types));
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
