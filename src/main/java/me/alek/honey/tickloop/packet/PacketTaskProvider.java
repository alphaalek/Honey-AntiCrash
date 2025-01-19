package me.alek.honey.tickloop.packet;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.*;
import me.alek.honey.util.functions.ChainRunnable;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MethodInvoker;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;

public class PacketTaskProvider {

    public static final PacketTaskProvider PROVIDER = new PacketTaskProvider();

    private static final boolean MODERN = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14);
    private static final BiConsumer<PacketContainer<? extends WrappedPacket<?>>, Throwable> DEBUGGER = (packet, thrown) -> {
        HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE,
                "Error occurred when processing packet task " + packet.getType() + " by " + packet.getPlayerName(), thrown);
    };

    private static final AtomicReference<MethodInvoker<Object>> factoryInvokerCache = new AtomicReference<>();
    private static final Provider.ProviderOneInput<Factory<Runnable>, Boolean> factoryProvider = RemoteProviderBridge.Factory.<Factory<Runnable>, Boolean>createWithOneInput()
            .createProviderRemotely(null, RemoteProviderUtils::isProviderMethod, RemoteProviderUtils.getNewProviderWithException(PacketTaskFactory.Provider.class));

    private static Factory<Runnable> factory;

    static {
        try {
            Object remoteFactory = factoryProvider.get(MODERN);

            Provider.ProviderGenericInput<Runnable> factoryMethodProvider = RemoteProviderBridge.Factory.<Runnable>createWithGenericInput()
                    .createProviderRemotely(factoryInvokerCache, PacketTaskProvider::isFactoryMethod, remoteFactory);

            PacketTaskFactory.Adapter adapter = new PacketTaskFactory.Adapter(factoryMethodProvider);
            factory = adapter.get();

        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to setup packet task env", ex);
        }
    }

    private static boolean isFactoryMethod(AtomicInteger index, MethodInvoker<?> invoker) {
        return invoker.getName().equals("create");
    }

    public interface Factory<T> {

        T create(AtomicReference<Runnable> task,
                 AtomicReference<BooleanSupplier> check,
                 AtomicReference<Runnable> pre,
                 AtomicReference<Runnable> post,
                 AtomicReference<Consumer<Throwable>> catcher);
    }

    private interface Recyclable<T> {

        T recycle(Runnable task,
                  BooleanSupplier check,
                  Runnable pre,
                  Runnable post,
                  Consumer<Throwable> catcher);
    }

    private final Queue<Recyclable<Runnable>> recyclables = new LinkedList<>();

    public WrappedPacketTask create(Runnable task, PacketContainer<? extends WrappedPacket<?>> packet, int tickAdded) {
        WrappedPacketTask wrapped = new WrappedPacketTask(packet, tickAdded);

        Runnable packetTask = getPacketTask(task, wrapped::runCheck, wrapped::runPre, wrapped::runPost, thrown -> DEBUGGER.accept(packet, thrown));
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
