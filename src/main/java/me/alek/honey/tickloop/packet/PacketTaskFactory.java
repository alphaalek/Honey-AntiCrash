package me.alek.honey.tickloop.packet;

import me.alek.honey.util.reflection.MethodInvoker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class PacketTaskFactory implements PacketTaskProvider.Factory<Runnable> {

    public static boolean isFactoryMethod(AtomicInteger index, MethodInvoker<?> invoker) {
        return invoker.getName().equals("create");
    }

    private final boolean modern;

    public PacketTaskFactory(boolean modern) {
        this.modern = modern;
    }

    @Override
    public Runnable create(AtomicReference<Runnable> task,
                           AtomicReference<BooleanSupplier> check,
                           AtomicReference<Runnable> pre,
                           AtomicReference<Runnable> post,
                           AtomicReference<Consumer<Throwable>> catcher) {
        PacketTask packetTask = new PacketTask(task, check, pre, post, catcher);
        if (modern) {
            return packetTask;
        } else {
            return new PacketTask.TaskWrapper1_13_2<>(packetTask);
        }
    }

    public static class Provider implements me.alek.honey.util.Provider.ProviderOneInput<PacketTaskFactory, Boolean> {

        @Override
        public PacketTaskFactory get(Boolean modern) {
            return new PacketTaskFactory(modern);
        }
    }

    public static class Adapter implements me.alek.honey.util.Provider.ProviderNoInput<PacketTaskProvider.Factory<Runnable>> {

        private final ProviderGenericInput<Runnable> factoryProvider;

        public Adapter(ProviderGenericInput<Runnable> factoryProvider) {
            this.factoryProvider = factoryProvider;
        }

        @Override
        public PacketTaskProvider.Factory<Runnable> get() {
            return (task, check, pre, post, catcher) -> {
                return factoryProvider.get(new Object[]{task, check, pre, post, catcher});
            };
        }
    }
}
