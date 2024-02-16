package me.alek.honey.tracker;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.utility.MethodInvoker;
import me.alek.honey.netty.utility.Reflection;
import me.alek.honey.utility.ThreadLocalAccessor;
import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.logging.Logger;

public class QueueExecutionWatchdogThread extends Thread {

    private static QueueExecutionWatchdogThread instance;
    private static final long timeoutTime = 60000L;
    private static final long verificationTime = 30000L;

    private volatile long lastCall = System.currentTimeMillis();
    private final ThreadLocalAccessor<Long> lastVerification = new ThreadLocalAccessor<>();
    private boolean running;

    public QueueExecutionWatchdogThread() {
        instance = this;
    }

    public static void startWatchdog() {
        if (instance != null && !instance.running)
            instance.start();
    }

    public static void stopWatchdog() {
        if (instance != null)
            instance.running = false;
    }

    public static void verify(long time) {
        if (instance != null)
            instance.lastVerification.set(time);
    }

    public static void clearVerification() {
        if (instance != null)
            instance.lastVerification.set(0L);
    }

    public static void call(long time) {
        if (instance != null)
            instance.lastCall = time;
    }

    @Override
    public void run() {
        running = true;
        while (running) {

            long time = System.currentTimeMillis();
            if (lastCall != 0 && time - lastCall > timeoutTime) {

                boolean invalid = false;
                for (long lastVerification : this.lastVerification.getAll()) {
                    long diff = time - lastVerification;
                    if (lastVerification != 0 && diff > verificationTime - 1500L && diff < verificationTime + 1500L) {
                        invalid = true;
                        break;
                    }
                }
                if (invalid && lastVerification.getSize() > 0) {
                    dumpThreads();
                    restartServer();
                }
            }

            try {
                Thread.sleep(1000L);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void dumpThreads() {
        MethodInvoker<Object> method = Reflection.getMethod(
                Reflection.getClass("org.spigotmc.WatchdogThread"), "dumpThread", ThreadInfo.class, Logger.class);

        ThreadInfo serverThreadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(
                HoneyPlugin.get().getServerThread().getId(), Integer.MAX_VALUE);
        ThreadInfo[] allThreads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);

        try {
            method.invoke(null, serverThreadInfo, Bukkit.getServer().getLogger());

            for (ThreadInfo threadInfo : allThreads) {
                method.invoke(null, threadInfo, Bukkit.getServer().getLogger());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void restartServer() {
        Reflection.getMethod(Reflection.getClass("org.spigotmc.RestartCommand"), "restart")
                .invoke(null);
    }
}
