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
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.Identifiable;
import me.alek.honey.util.chain.ChainRunnable;
import me.alek.honey.util.chain.ChainSupplier;
import me.alek.honey.util.reflection.MethodInvoker;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

@SuppressWarnings("unchecked")
public class PacketTaskProvider {

    public static final Array<PacketTaskProvider> PROVIDERS = new Array<>();

    private static Factory<Runnable> factory;
    private static final AtomicReference<MethodInvoker<Runnable>> factoryInvokerCache = new AtomicReference<>();

    static {
        try {
            HoneyClassLoader honeyClassLoader = HoneyClassLoader.get();
            Thread.currentThread().setContextClassLoader(honeyClassLoader);

            Class<?> factoryClass = honeyClassLoader.loadClass(PacketTaskFactory.class.getName());

            Object factory = factoryClass.getDeclaredConstructor().newInstance();
            PacketTaskProvider.factory = createModuleContextFactory(factory);

        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to setup packet task env", ex);
        } finally {
            Thread.currentThread().setContextClassLoader(WrappedPacketTask.class.getClassLoader());
        }
    }

    interface Factory<T> {

        T create(AtomicReference<Runnable> task,
                 AtomicReference<Supplier<Boolean>> check,
                 AtomicReference<Runnable> pre,
                 AtomicReference<Runnable> post,
                 AtomicReference<Consumer<Throwable>> catcher);
    }

    interface Recyclable<T> {

        T recycle(Runnable task, Supplier<Boolean> check, Runnable pre, Runnable post, Consumer<Throwable> catcher);
    }

    private static Factory<Runnable> createModuleContextFactory(Object factory) {
        IStructureModifier<?> structure = ReflectStructureCache.acquireStructure(factory.getClass())
                .withTarget(factory);

        return (task, check, pre, post, catcher) -> {
            return (Runnable) structure.invokeMethod(factoryInvokerCache, 0, task, check, pre, post, catcher);
        };
    }

    private final IDRegistry ids = new IDRegistry();
    private final Array<ProviderUnsafe> unsafeWindows = new Array<>();
    private final Array<Recyclable<Runnable>> recyclables = new Array<>();

    public Array<ProviderUnsafe> getUnsafeWindows() {
        return unsafeWindows;
    }

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

        Supplier<Boolean> check = () -> packet.getPlayer().isStreamActive();
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

    public Runnable getPacketTask(Runnable task, Supplier<Boolean> check, Runnable pre, Runnable post, Consumer<Throwable> catcher) {
        Recyclable<Runnable> recyclable = recyclables.poll();
        if (recyclable != null) {
            return recyclable.recycle(task, check, pre, post, catcher);
        }

        return createPacketTask(task, check, pre, post, catcher);
    }

    public Runnable createPacketTask(Runnable task, Supplier<Boolean> check, Runnable pre, Runnable post, Consumer<Throwable> catcher) {
        int id = ids.acquire();

        AtomicReference<Runnable> taskProvider = registerTask(id, task);
        AtomicReference<Supplier<Boolean>> checkProvider = registerCheck(id, check);
        AtomicReference<Runnable> preProvider = registerPre(id, pre);
        AtomicReference<Runnable> postProvider = registerPost(id, post);
        AtomicReference<Consumer<Throwable>> catcherProvider = registerCatcher(id, catcher);

        injectRecyclablePost(task, taskProvider, checkProvider, preProvider, postProvider, catcherProvider);

        return factory.create(taskProvider, checkProvider, preProvider, postProvider, catcherProvider);
    }

    private void injectRecyclablePost(Runnable taskAdapter,
                                      AtomicReference<Runnable> taskProvider,
                                      AtomicReference<Supplier<Boolean>> checkProvider,
                                      AtomicReference<Runnable> preProvider,
                                      AtomicReference<Runnable> postProvider,
                                      AtomicReference<Consumer<Throwable>> catcherProvider) {
        postProvider.set(ChainRunnable.wrap(postProvider.get()).thenDo(() -> {
            recyclables.add(new Recyclable<Runnable>() {
                @Override
                public Runnable recycle(Runnable task, Supplier<Boolean> check, Runnable pre, Runnable post, Consumer<Throwable> catcher) {
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

    public <T> AtomicReference<T> setUnsafeProvider(int id, TaskLifecycleState state, T val) {
        return (AtomicReference<T>) unsafeWindows.get(id, ProviderUnsafe::new).jobProviders
                .setAndGet(Identifiable.of(state), new AtomicReference<>(val));
    }

    public AtomicReference<Runnable> registerTask(int id, Runnable task) {
        return setUnsafeProvider(id, TaskLifecycleState.TASK, task);
    }

    public AtomicReference<Supplier<Boolean>> registerCheck(int id, Supplier<Boolean> check) {
        return setUnsafeProvider(id, TaskLifecycleState.CHECK, check);
    }

    public AtomicReference<Runnable> registerPre(int id, Runnable pre) {
        return setUnsafeProvider(id, TaskLifecycleState.PRE, pre);
    }

    public AtomicReference<Runnable> registerPost(int id, Runnable post) {
        return setUnsafeProvider(id, TaskLifecycleState.POST, post);
    }

    public AtomicReference<Consumer<Throwable>> registerCatcher(int id, Consumer<Throwable> catcher) {
        return setUnsafeProvider(id, TaskLifecycleState.CATCH, catcher);
    }

    public static class ProviderUnsafe {

        private final Array<AtomicReference<?>> jobProviders = new Array<>();
    }
}
