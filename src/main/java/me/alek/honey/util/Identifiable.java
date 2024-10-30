package me.alek.honey.util;

import java.util.function.IntSupplier;

public interface Identifiable extends IntSupplier {

    int getID();

    default int getAsInt() {
        return getID();
    }

    static Identifiable of(Enum<?> enumVal) {
        return enumVal::ordinal;
    }
}
