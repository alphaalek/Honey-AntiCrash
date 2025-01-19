package me.alek.honey.util.functions;

public interface ChainRunnable extends Runnable {

    static ChainRunnable wrap(Runnable runnable) {
        return runnable::run;
    }

    default ChainRunnable thenDo(Runnable runnable) {
        return () -> {
            ChainRunnable.this.run();
            runnable.run();
        };
    }
}
