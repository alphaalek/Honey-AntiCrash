package me.alek.honey.async;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.lang.Lang;
import net.kyori.adventure.text.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class AsyncPacketStream implements TaskSupplier<Object> {

    private final AtomicLong lastHandledTask = new AtomicLong();
    private final HoneyPlayer player;
    private final WorkerPool<Object> workerPool;

    private ChainedCallable<Object> currentTask;
    private Worker<Object> worker;
    private boolean unregistered;

    public AsyncPacketStream(HoneyPlayer player, WorkerPool<Object> workerPool) {
        this.player = player;
        this.workerPool = workerPool;

        // TODO: rework
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    @Override
    public ChainedCallable<Object> getTask() {
        return currentTask;
    }

    @Override
    public void submitTask(ChainedCallable<Object> task) {
        if (unregistered) {
            return;
        }
        currentTask = task;

        if (lastHandledTask.get() == 0) {
            lastHandledTask.set(System.currentTimeMillis());
        }
        currentTask.addListener(d -> lastHandledTask.set(System.currentTimeMillis()));

        if (worker != null && worker.isActive()) {
            worker.submit(this);
        }

        if (worker != null && !worker.isActive()) {
            worker = workerPool.relocateWorkerTasks(this, worker);
        }

        if (worker == null) {
            worker = workerPool.submit(this);
        }

        if (worker.isQuarantined()) {
            if (worker.relocate(this)) {
                worker = workerPool.relocateWorkerTasks(this, worker);
                return;
            }

            if (player.isBukkitReady()) {
                long time = System.currentTimeMillis() - lastHandledTask.get();
                if (time > 200) {
                    HoneyPlugin.get().getNotificationService().showQuarantinedAsyncWorker(player, System.currentTimeMillis() - lastHandledTask.get());
                }
                if (time > 10_000) {
                    player.kick(Lang.get("kick.too.long.packet.handle"));
                }
            }
        }
    }

    public synchronized void decode(Consumer<Object> consumer) {
        submitTask(new ChainedCallable<>(this, () -> null).addListener(consumer));
    }

    public synchronized void prependDecode(Consumer<Object> consumer) {
        if (currentTask == null) {
            consumer.accept(null);
        } else if (currentTask.isCalled()) {
            consumer.accept(currentTask.getValue());
        } else {
            synchronized (currentTask) {
                if (currentTask == null || currentTask.isCalled()) {
                    prependDecode(consumer);
                    return;
                }

                currentTask.addListener(consumer);
            }
        }
    }

    public synchronized void farewell() {
        if (worker != null) {
            unregistered = true;
            worker.decrementSupplierCount();
        }

        worker = null;
    }
}
