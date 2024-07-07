package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyClassLoader;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import me.alek.honey.util.chain.ChainRunnable;
import me.alek.honey.util.chain.ChainSupplier;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MethodInvoker;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;

public class PacketTaskProvider {

    public static final Array<PacketTaskProvider> PROVIDERS = new Array<>();
    private static final boolean MODERN = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14);

    private static final AtomicReference<MethodInvoker<Runnable>> factoryInvokerCache = new AtomicReference<>();
    private static Factory<Runnable> factory;

    static {
        try {
            HoneyClassLoader honeyClassLoader = HoneyClassLoader.get();
            Thread.currentThread().setContextClassLoader(honeyClassLoader);

            Class<?> factoryClass = honeyClassLoader.loadClass(PacketTaskFactory.class.getName());

            Object factory = factoryClass.getDeclaredConstructor(boolean.class).newInstance(MODERN);
            PacketTaskProvider.factory = createModuleContextFactory(factory);

        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to setup packet task env", ex);
        } finally {
            Thread.currentThread().setContextClassLoader(WrappedPacketTask.class.getClassLoader());
        }
    }

    interface Factory<T> {

        T create(AtomicReference<Runnable> task,
                 AtomicReference<BooleanSupplier> check,
                 AtomicReference<Runnable> pre,
                 AtomicReference<Runnable> post,
                 AtomicReference<Consumer<Throwable>> catcher);
    }

    interface Recyclable<T> {

        T recycle(Runnable task, BooleanSupplier check, Runnable pre, Runnable post, Consumer<Throwable> catcher);
    }

    private static Factory<Runnable> createModuleContextFactory(Object factory) {
        IStructureModifier<?> structure = ReflectStructureCache.acquireStructure(factory.getClass())
                .withTarget(factory);

        return (task, check, pre, post, catcher) -> {
            return (Runnable) structure.invokeMethod(factoryInvokerCache, 0, task, check, pre, post, catcher);
        };
    }

    private final Queue<Recyclable<Runnable>> recyclables = new LinkedList<>();

    public static PacketTaskProvider getProvider(HoneyPlayer player) {
        return PROVIDERS.get(player,
                ChainSupplier.wrap(PacketTaskProvider::new).thenDo(() -> {
                    player.leaveCallback.addAction(Callback.Action.<HoneyPlayer>from(p -> {
                        PacketTaskProvider.PROVIDERS.removeAndGet(player);
                    }));
                })
        );
    }

    public WrappedPacketTask create(Runnable task, PacketContainer<? extends WrappedPacket<?>> packet, int tickAdded) {
        WrappedPacketTask wrapped = new WrappedPacketTask(packet, tickAdded);

        BooleanSupplier check = () -> packet.getPlayer().isStreamActive();
        Runnable pre = () -> {
            wrapped.setStarted();
            TaskLifecycleHandler.registerTask(wrapped);
        };
        Runnable post = () -> {
            wrapped.done();
            packet.invokeServerCallback();

            TaskLifecycleHandler.unregisterTask(packet);
        };
        Consumer<Throwable> catcher = thrown -> {
            HoneyPlugin.get().getLoggerService().getLogger().log(Level.SEVERE,
                    "Error occurred when processing packet task " + packet.getType() + " by " + packet.getPlayerName(), thrown);
        };

        Runnable packetTask = getPacketTask(task, check, pre, post, catcher);
        wrapped.initPacketTask(packetTask);

        return wrapped;
    }

    public Runnable getPacketTask(Runnable task, BooleanSupplier check, Runnable pre, Runnable post, Consumer<Throwable> catcher) {
        Recyclable<Runnable> recyclable = recyclables.poll();
        if (recyclable != null) {
            return recyclable.recycle(task, check, pre, post, catcher);
        }

        return createPacketTask(task, check, pre, post, catcher);
    }

    public Runnable createPacketTask(Runnable task, BooleanSupplier check, Runnable pre, Runnable post, Consumer<Throwable> catcher) {
        AtomicReference<Runnable> taskProvider = new AtomicReference<>(task);
        AtomicReference<BooleanSupplier> checkProvider = new AtomicReference<>(check);
        AtomicReference<Runnable> preProvider = new AtomicReference<>(pre);
        AtomicReference<Runnable> postProvider = new AtomicReference<>(post);
        AtomicReference<Consumer<Throwable>> catcherProvider = new AtomicReference<>(catcher);

        Runnable packetTask = factory.create(taskProvider, checkProvider, preProvider, postProvider, catcherProvider);
        injectRecyclablePost(packetTask, taskProvider, checkProvider, preProvider, postProvider, catcherProvider);

        return packetTask;
    }

    private void injectRecyclablePost(Runnable taskAdapter,
                                      AtomicReference<Runnable> taskProvider,
                                      AtomicReference<BooleanSupplier> checkProvider,
                                      AtomicReference<Runnable> preProvider,
                                      AtomicReference<Runnable> postProvider,
                                      AtomicReference<Consumer<Throwable>> catcherProvider) {
        postProvider.set(ChainRunnable.wrap(postProvider.get()).thenDo(() -> {
            recyclables.add(new Recyclable<Runnable>() {
                @Override
                public Runnable recycle(Runnable task, BooleanSupplier check, Runnable pre, Runnable post, Consumer<Throwable> catcher) {
                    taskProvider.set(task);
                    checkProvider.set(check);
                    preProvider.set(pre);
                    postProvider.set(post);
                    catcherProvider.set(catcher);

                    injectRecyclablePost(taskAdapter, taskProvider, checkProvider, preProvider, postProvider, catcherProvider);
                    return taskAdapter;
                }
            });
        }));
    }
}
