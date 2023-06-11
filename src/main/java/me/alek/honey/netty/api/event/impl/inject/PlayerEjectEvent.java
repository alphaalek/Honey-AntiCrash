package me.alek.honey.netty.api.event.impl.inject;

import org.bukkit.entity.Player;

public class PlayerEjectEvent extends PlayerBoundInjectEvent {

    public PlayerEjectEvent(Player player, InjectType injectType, InjectCallback callback) {
        super(player, injectType, callback, InjectBound.PLAYER);
    }
}
