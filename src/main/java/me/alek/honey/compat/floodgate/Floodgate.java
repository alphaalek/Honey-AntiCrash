package me.alek.honey.compat.floodgate;

import me.alek.honey.netty.packetwrappers.impl.login.WrappedLoginInLoginStart;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.Optional;

public class Floodgate {

    private static final FloodgateApi FLOODGATE = FloodgateApi.getInstance();

    public static final String DATA_HANDLER = "floodgate_data_handler";

    public static String getFloodgateUsername(WrappedLoginInLoginStart hello) {
        if (FLOODGATE != null) {
            Optional<FloodgatePlayer> floodgatePlayerOptional = FLOODGATE.getPlayers().stream()
                    .filter(p -> p.getUsername().equals(hello.getUsername()))
                    .findFirst();
            return floodgatePlayerOptional.map(FloodgatePlayer::getCorrectUsername).orElse(null);
        }

        return null;
    }
}
