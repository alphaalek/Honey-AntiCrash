package me.alek.honey.util.id;

public interface IntIdentifiable extends Identifiable<Integer> {

    static IntIdentifiable of(Enum<?> enumVal) {
        return enumVal::ordinal;
    }
}
