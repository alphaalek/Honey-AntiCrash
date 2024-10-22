package me.alek.honey.netty.packet;

public interface Cancellable {

    void cancel();

    boolean isCancelled();

    class Trigger implements Cancellable {

        private boolean cancelled;

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}
