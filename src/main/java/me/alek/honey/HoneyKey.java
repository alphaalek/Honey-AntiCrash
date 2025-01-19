package me.alek.honey;

import me.alek.honey.util.Identifiable;

import java.util.Objects;

public class HoneyKey implements Identifiable, Cloneable {

    private final int key;
    private final String identifier;

    protected HoneyKey(int key, String identifier) {
        this.key = key;
        this.identifier = identifier;
    }

    public static HoneyKey of(int key) {
        return new HoneyKey(key, null);
    }

    public static HoneyKey of(int key, String identifier) {
        return new HoneyKey(key, identifier);
    }

    public static HoneyKey of(Identifiable identifiable) {
        return of(identifiable.getID());
    }

    public int getRegistryId() {
        return key;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object to) {
        if (!(to instanceof HoneyKey)) {
            return false;
        }

        HoneyKey key = (HoneyKey) to;
        return Objects.equals(key.identifier, this.identifier)
                && key.key == this.key;
    }

    @Override
    public HoneyKey clone() {
        try {
            return (HoneyKey) super.clone();
        } catch (Exception ex) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        return "id=" + key + ",identifier=" + identifier;
    }

    @Override
    public int getID() {
        return getRegistryId();
    }
}