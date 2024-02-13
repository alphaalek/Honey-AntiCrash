package me.alek.honey.monitor;

public interface IndependentMonitorer<T> extends Monitorer {

    String getMessage(T input);

    MonitorSendAt getSendAt();
}
