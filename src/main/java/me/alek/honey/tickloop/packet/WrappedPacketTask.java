package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyClassLoader;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.reflection.MethodInvoker;
import org.bukkit.Bukkit;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

@SuppressWarnings("unchecked")
public class WrappedPacketTask {

    private static Factory<FutureTask<Object>> factory;
    private static final AtomicReference<MethodInvoker<FutureTask<Object>>> factoryInvokerCache = new AtomicReference<>();

    static {
        try {
            HoneyClassLoader honeyClassLoader = HoneyClassLoader.get();
            Thread.currentThread().setContextClassLoader(honeyClassLoader);

            Class<?> factoryClass = honeyClassLoader.loadClass(PacketTaskFactory.class.getName());

            Object factory = factoryClass.getDeclaredConstructor().newInstance();
            WrappedPacketTask.factory = createModuleContextFactory(factory);

        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to setup packet task env", ex);
        } finally {
            Thread.currentThread().setContextClassLoader(WrappedPacketTask.class.getClassLoader());
        }
    }

    interface Factory<T> {

        T create(Runnable task, Supplier<Boolean> check, Runnable pre, Runnable post, Consumer<Throwable> catcher);
    }

    private static Factory<FutureTask<Object>> createModuleContextFactory(Object factory) {
        IStructureModifier<?> structure = ReflectStructureCache.acquireStructure(factory.getClass())
                .withTarget(factory);

        return (task, check, pre, post, catcher) -> {
            return (FutureTask<Object>) structure.invokeMethod(factoryInvokerCache, 0, task, check, pre, post, catcher);
        };
    }

    public static WrappedPacketTask create(Runnable task, PacketContainer<? extends WrappedPacket<?>> packet, int tickAdded) {
        WrappedPacketTask wrapped = new WrappedPacketTask(packet, tickAdded);

        FutureTask<Object> packetTask = factory.create(task
            ,  () -> packet.getPlayer().isStreamActive()
            ,  () -> {
                wrapped.started = System.nanoTime();
                PacketTaskHandler.registerTask(wrapped);
            }, () -> {
                wrapped.done = true;
                packet.invokeServerCallback();

                PacketTaskHandler.unregisterTask(packet);
            }, thrown -> {
                HoneyPlugin.get().getLoggerService().getLogger().log(Level.SEVERE,
                        "Error occurred when processing packet task " + packet.getType() + " by " + packet.getPlayerName(), thrown);
            }
        );

        wrapped.initPacketTask(packetTask);
        return wrapped;
    }

    private final int tickAdded;
    private final PacketContainer<? extends WrappedPacket<?>> packet;
    private FutureTask<Object> packetTask;
    private boolean postponed;
    private long started;
    private boolean done;

    public WrappedPacketTask(PacketContainer<? extends WrappedPacket<?>> packet, int tickAdded) {
        this.packet = packet;
        this.tickAdded = tickAdded;
    }

    public void initPacketTask(FutureTask<Object> packetTask) {
        this.packetTask = packetTask;
    }

    public HoneyPlayer getPlayer() {
        return packet.getPlayer();
    }

    public PacketContainer<? extends WrappedPacket<?>> getPacket() {
        return packet;
    }

    public FutureTask<Object> getPacketTask() {
        return packetTask;
    }

    public long getStarted() {
        return started;
    }

    public boolean isPostponed() {
        return postponed;
    }

    public void setPostponed() {
        postponed = true;
    }

    public int getTickAdded() {
        return tickAdded;
    }

    public boolean isDone() {
        return done;
    }
}
