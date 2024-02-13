package me.alek.honey.netty;

import me.alek.honey.netty.api.event.Event;
import me.alek.honey.netty.api.event.EventManager;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.bukkit.BukkitEventInternal;
import me.alek.honey.netty.injector.AbstractNettyChannelInjector;
import me.alek.honey.netty.injector.EarlyChannelInjector;
import me.alek.honey.netty.injector.LateChannelInjector;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.PacketWrapperInvokerFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.processor.InternalPacketProcessor;
import me.alek.honey.netty.utility.NetProtocol;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class PacketLibrary {

    private final PacketLibrarySettings settings;
    private final PacketProcessor packetProcessor;
    private final EventManager eventManager;
    private final Plugin plugin;
    private CountDownLatch disablingLatch;
    private AbstractNettyChannelInjector proxy;
    private boolean enabled = true;

    private static PacketLibrary INSTANCE;

    public static PacketLibrary get() {
        return INSTANCE;
    }

    public static PacketLibrary set(PacketLibrarySettings settings) {
        if (INSTANCE == null) new PacketLibrary(settings);
        return INSTANCE;
    }

    private PacketLibrary(PacketLibrarySettings settings) {
        INSTANCE = this;
        this.settings = settings;
        plugin = settings.getPlugin();
        eventManager = new EventManager();
        Bukkit.getServer().getPluginManager().registerEvents(new BukkitEventInternal(), settings.getPlugin());

        if (useLateInjection())
            proxy = new LateChannelInjector();
        else
            proxy = new EarlyChannelInjector();

        proxy.inject();
        packetProcessor = new InternalPacketProcessor(eventManager);

        PacketType.load();
        PacketWrapperInvokerFactory.load();
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

    public AbstractNettyChannelInjector getInjector() {
        return proxy;
    }

    public boolean useLateInjection() {
        return settings.useLateInjection();
    }

    public NetProtocol getFallbackProtocol() {
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
        return packetProcessor;
    }

    public void waitUntilDisabledIfProcessing() {
        if (disablingLatch == null) return;

        try {
            disablingLatch.await();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendPacket(Player player, WrappedPacket<?> packet) {
        proxy.writePacket(player, packet.getHandle());
    }

    public void flushPackets(Player player) {
        proxy.flushPackets(player);
    }

    public <WP extends WrappedPacket<WP>> void addListener(SyncPacketAdapter<WP> adapter, ListenerPriority priority, Supplier<PacketTypeEnum> packetTypes) {
        packetProcessor.addListener(adapter, priority, packetTypes.get());
    }

    public <WP extends WrappedPacket<WP>> void addListeners(SyncPacketAdapter<WP> adapter, ListenerPriority priority, Supplier<List<PacketTypeEnum>> packetTypes) {
        packetProcessor.addListener(adapter, priority, packetTypes.get());
    }

    public void addFuzzyListeners(FuzzyPacketAdapter adapter, ListenerPriority priority, Supplier<List<PacketTypeEnum>> packetTypes) {
        packetProcessor.addListener(adapter, priority, packetTypes.get());
    }

    public void addDecodeListener(SyncPacketAdapter<?> adapter, ListenerPriority priority) {
        packetProcessor.addDecodeListener(adapter, priority);
    }

    public <WP extends WrappedPacket<WP>> void removeListener(SyncPacketAdapter<WP> adapter) {
        removeListeners(adapter, PacketType.getTypesAvailableFor(NetProtocol.getProtocol()));
    }

    public <WP extends WrappedPacket<WP>> void removeListeners(SyncPacketAdapter<WP> adapter, List<PacketTypeEnum> packetTypes) {
        packetProcessor.removeListener(adapter, packetTypes);
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
        return packetProcessor.isRegistered(adapter);
    }

    @SuppressWarnings("unchecked")
    public boolean isFuzzyRegistered(FuzzyPacketAdapter adapter) {
        return isRegistered(adapter);
    }

    public void setPostAction(PacketTypeEnum packetType, Runnable postAction) {
        packetProcessor.setPostAction(packetType, postAction);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        enabled = false;
        disablingLatch = new CountDownLatch(1);
        proxy.eject();
        disablingLatch.countDown();
        disablingLatch = null;
    }
}
