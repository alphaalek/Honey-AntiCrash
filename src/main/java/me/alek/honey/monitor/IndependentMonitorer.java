package me.alek.honey.monitor;

import java.util.List;

public interface IndependentMonitorer<T> extends Monitorer {

    String getMessage(T input);

    List<SendProperty> getSendProperties();
}
