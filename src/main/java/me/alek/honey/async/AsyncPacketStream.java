package me.alek.honey.async;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.lang.Lang;

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
        return this.player;
    }

    @Override
    public ChainedCallable<Object> getTask() {
        return this.currentTask;
    }

    @Override
    public void submitTask(ChainedCallable<Object> task) {
        if (this.unregistered) {
            return;
        }
        this.currentTask = task;

        if (this.lastHandledTask.get() == 0) {
            this.lastHandledTask.set(System.currentTimeMillis());
        }
        this.currentTask.addListener(d -> this.lastHandledTask.set(System.currentTimeMillis()));

        if (this.worker != null && this.worker.isActive()) {
            this.worker.submit(this);
        }

        if (this.worker != null && !this.worker.isActive()) {
            this.worker = this.workerPool.relocateWorkerTasks(this, this.worker);
        }

        if (this.worker == null) {
            this.worker = this.workerPool.submit(this);
        }

        if (this.worker.isQuarantined()) {
            if (this.worker.relocate(this)) {
                this.worker = this.workerPool.relocateWorkerTasks(this, worker);
                return;
            }

            if (this.player.linkedBukkit()) {
                long time = System.currentTimeMillis() - this.lastHandledTask.get();
                if (time > 200) {
                    HoneyPlugin.get().getNotificationService().showQuarantinedAsyncWorker(this.player, System.currentTimeMillis() - lastHandledTask.get());
                }
                if (time > 10_000) {
                    this.player.kick(Lang.get("kick.too.long.packet.handle"));
                }
            }
        }
    }

    public synchronized void decode(Consumer<Object> consumer) {
        submitTask(new ChainedCallable<>(this, () -> null).addListener(consumer));
    }

    public synchronized void prependDecode(Consumer<Object> consumer) {
        if (this.currentTask == null) {
            consumer.accept(null);
        } else if (this.currentTask.isCalled()) {
            consumer.accept(this.currentTask.getValue());
        } else {
            if (this.currentTask == null || this.currentTask.isCalled()) {
                prependDecode(consumer);
                return;
            }

            this.currentTask.addListener(consumer);
        }
    }

    public synchronized void farewell() {
        if (this.worker != null) {
            this.unregistered = true;
            this.worker.decrementSupplierCount();
        }

        this.worker = null;
    }
}
