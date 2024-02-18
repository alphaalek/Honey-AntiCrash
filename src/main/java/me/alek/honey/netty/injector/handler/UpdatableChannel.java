package me.alek.honey.netty.injector.handler;

import org.bukkit.entity.Player;

public interface UpdatableChannel {

    void setPlayer(Player player);

    Player getPlayer();
}
