package me.alek.honey.netty;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.NettyChannelProxy;
import me.alek.honey.netty.api.event.Event;
import me.alek.honey.netty.api.event.EventManager;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.bukkit.BukkitEventInternal;
import me.alek.honey.netty.injector.EarlyChannelInjector;
import me.alek.honey.netty.injector.LateChannelInjector;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.cache.PacketWrapperFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.processor.InternalPacketProcessor;
import me.alek.honey.netty.utility.AsyncFuture;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Supplier;

public class PacketLibrary {

    private final PacketLibrarySettings settings;
    private PacketProcessor internalPacketProcessor;
    private final EventManager eventManager;
    private AsyncFuture future;
    private final JavaPlugin plugin;
    private NettyChannelProxy proxy;

    private static PacketLibrary INSTANCE;

    public static PacketLibrary get() {
        return INSTANCE;
    }

    public static PacketLibrary set(PacketLibrarySettings settings) {
        new PacketLibrary(settings);
        return INSTANCE;
    }

    private PacketLibrary(PacketLibrarySettings settings) {
        INSTANCE = this;
        this.settings = settings;
        this.plugin = settings.getPlugin();
        eventManager = new EventManager();
        Bukkit.getServer().getPluginManager().registerEvents(new BukkitEventInternal(), HoneyPlugin.get());

        Bukkit.getLogger().info("use late injection: " + useLateInjection());
        if (useLateInjection()) {
            proxy = new LateChannelInjector();
        }
        else {
            proxy = new EarlyChannelInjector();
        }
        Bukkit.getLogger().info("using proxy: " + proxy);
        proxy.inject();
        Bukkit.getLogger().info("3");
        internalPacketProcessor = new InternalPacketProcessor(eventManager);

        future = PacketType.load();
        Bukkit.getLogger().info("4");
        future.join(PacketWrapperFactory.load());
    }

    public void callSyncEvent(Event event, boolean isPacket) {
        eventManager.callListeners(event, isPacket);
    }


    public void setLateProxy() {
        if (proxy instanceof EarlyChannelInjector) {
            proxy.eject();
            proxy = new LateChannelInjector();
            proxy.inject();
        }
        settings.setUseLateInjection(true);
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public NettyChannelProxy getProxy() {
        return proxy;
    }

    public boolean useLateInjection() {
        return settings.useLateInjection();
    }

    public Protocol getFallbackProtocol() {
        return settings.getFallbackProtocol();
    }

    public String getHandlerName() {
        return settings.getHandlerName();
    }

    public String getDecoderName() {
        return settings.getDecoderName();
    }

    public PacketLibrarySettings getSettings() {
        return settings;
    }

    public PacketProcessor getPacketProcessor() {
        return internalPacketProcessor;
    }

    private void addListener(Runnable runnable) {
        if (future.isDone()) {
            runnable.run();
            return;
        }
        future.addListener(runnable);
    }


    public void sendPacket(Player player, WrappedPacket<?> packet) {
        proxy.writePacket(player, packet);
    }

    public void flushPackets(Player player) {
        proxy.flushPackets(player);
    }

    public <WP extends WrappedPacket<WP>> void addListener(SyncPacketAdapter<WP> adapter, ListenerPriority priority, Supplier<PacketTypeEnum> packetTypes) {
        addListener(() -> internalPacketProcessor.addListener(adapter, priority, packetTypes.get()));
    }

    public <WP extends WrappedPacket<WP>> void addListeners(SyncPacketAdapter<WP> adapter, ListenerPriority priority, Supplier<List<PacketTypeEnum>> packetTypes) {
        addListener(() -> internalPacketProcessor.addListener(adapter, priority, packetTypes.get()));
    }

    public void addFuzzyListeners(FuzzyPacketAdapter adapter, ListenerPriority priority, Supplier<List<PacketTypeEnum>> packetTypes) {
        addListener(() -> internalPacketProcessor.addListener(adapter, priority, packetTypes.get()));
    }

    public <WP extends WrappedPacket<WP>> void removeListener(SyncPacketAdapter<WP> adapter) {
        removeListeners(adapter, PacketType.getTypesAvailableFor(Protocol.getProtocol()));
    }

    public <WP extends WrappedPacket<WP>> void removeListeners(SyncPacketAdapter<WP> adapter, List<PacketTypeEnum> packetTypes) {
        internalPacketProcessor.removeListener(adapter, packetTypes);
    }

    @SuppressWarnings("unchecked")
    public void removeFuzzyListener(FuzzyPacketAdapter adapter) {
        removeListener(adapter);
    }

    @SuppressWarnings("unchecked")
    public void removeFuzzyListeners(FuzzyPacketAdapter adapter, List<PacketTypeEnum> packetTypes) {
        removeListeners(adapter, packetTypes);
    }

    public <WP extends WrappedPacket<WP>> boolean isRegistered(SyncPacketAdapter<WP> adapter) {
        return internalPacketProcessor.isRegistered(adapter);
    }

    @SuppressWarnings("unchecked")
    public boolean isFuzzyRegistered(FuzzyPacketAdapter adapter) {
        return isRegistered(adapter);
    }

    public void setPostAction(PacketTypeEnum packetType, Runnable postAction) {
        internalPacketProcessor.setPostAction(packetType, postAction);
    }

    public void disable() {
        proxy.eject();
    }


}
