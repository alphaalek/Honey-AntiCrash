package me.alek.honey.netty;

import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.bukkit.BukkitEventInternal;
import me.alek.honey.netty.injector.ServerPacketManager;
import me.alek.honey.netty.injector.connection.SpigotPipelineInjector;
import me.alek.honey.netty.injector.connection.SpigotServerInjector;
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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class PacketLibrary {

    private final PacketLibrarySettings settings;
    private final PacketProcessor packetProcessor;
    private final Plugin plugin;
    private CountDownLatch disablingLatch;
    private final SpigotPipelineInjector pipelineInjector;
    private final SpigotServerInjector serverInjector;
    private final ServerPacketManager packetManager;
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
        Bukkit.getServer().getPluginManager().registerEvents(new BukkitEventInternal(), settings.getPlugin());

        serverInjector = new SpigotServerInjector();
        if (serverInjector.isServerBound()) {
            serverInjector.inject();
        }

        pipelineInjector = new SpigotPipelineInjector();

        packetProcessor = new InternalPacketProcessor();
        packetManager = new ServerPacketManager();

        PacketType.load();
        PacketWrapperInvokerFactory.load();
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

    public NetProtocol getFallbackProtocol() {
        return settings.getFallbackProtocol();
    }

    public String getHandlerName() {
        return settings.getHandlerName();
    }

    public String getDecoderName() {
        return settings.getDecoderName();
    }

    public String getConnectionName() {
        return settings.getConnectionName();
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

    public void writePacket(Player player, WrappedPacket<?> packet) {
        packetManager.writePacket(player, packet.getHandle());
    }

    public void flushPackets(Player player) {
        packetManager.flushPackets(player);
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
        serverInjector.eject();
        disablingLatch.countDown();
        disablingLatch = null;
    }
}
