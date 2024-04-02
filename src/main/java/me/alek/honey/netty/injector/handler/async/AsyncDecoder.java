package me.alek.honey.netty.injector.handler.async;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.ServerPacketManager;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.netty.utility.Reflection;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutKickDisconnect;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class AsyncDecoder implements TaskSupplier<Object> {

    private HoneyPlayer player;
    private final WorkerPool<Object> workerPool;
    private ChainedCallable<Object> currentTask;
    public Worker<Object> worker;
    private boolean unregistered;
    private final AtomicLong lastHandledTask = new AtomicLong();

    public AsyncDecoder(HoneyPlayer player, WorkerPool<Object> workerPool) {
        this.player = player;
        this.workerPool = workerPool;
    }

    public AsyncDecoder(WorkerPool<Object> workerPool) {
        this(null, workerPool);
    }

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public synchronized void decode(Consumer<Object> consumer) {
        submitTask(new ChainedCallable<>(this, () -> null).addListener(consumer));
    }

    public synchronized void encode(Consumer<Object> consumer) {
        if (currentTask == null || currentTask.isCalled())
            consumer.accept(currentTask);
        else {
            synchronized (currentTask) {
                if (currentTask.isCalled()) {
                    consumer.accept(currentTask);
                    return;
                }

                currentTask.addListener(consumer);
            }
        }
    }

    @Override
    public void submitTask(ChainedCallable<Object> task) {
        if (unregistered) {
            return;
        }

        if (lastHandledTask.get() == 0) {
            lastHandledTask.set(System.currentTimeMillis());
        }
        currentTask = task;
        currentTask.addListener((Consumer<Object>) (d) -> lastHandledTask.set(System.currentTimeMillis()));

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

            if (player != null) {
                long time = System.currentTimeMillis() - lastHandledTask.get();

                if (time > 200) {
                    HoneyPlugin.get().getNotificationService().sendActionBar(player, "§cAsync worker moved to quarantine... (" + (System.currentTimeMillis() - lastHandledTask.get()) + "ms)");
                }
                if (time > 10_000) {
                    kickPlayer();
                }
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

    private void kickPlayer() {
        if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_16_5)) {
            player.channel.close();
            return;
        }

        ServerPacketManager packetManager = HoneyPlugin.get().getPacketLibrary().getPacketManager();
        WrappedPacket<WrappedPlayOutKickDisconnect> kickDisconnect = PacketFactory.createNewPacket(PacketType.Play.Server.KICK_DISCONNECT, player);
        kickDisconnect.getContainer().getObjects(NMS.getIChatBaseComponentClass())
                .writeField(0, Reflection.getConstructor(Reflection.getClass("{nms}.ChatComponentText"), String.class).invoke(
                        "Packet task took way too long to be handled. The server is completely fine though :-)"
                ));

        try {
            packetManager.writeForcedPacket(player, kickDisconnect);
        } catch (Exception ex) {
            ex.printStackTrace();
            player.channel.close();
        }
    }

    @Override
    public ChainedCallable<Object> getTask() {
        return currentTask;
    }
}
