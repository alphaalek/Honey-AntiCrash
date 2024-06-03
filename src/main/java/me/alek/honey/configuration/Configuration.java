package me.alek.honey.configuration;

public interface Configuration {

    void reload();

    boolean isMisconfigured();
}
