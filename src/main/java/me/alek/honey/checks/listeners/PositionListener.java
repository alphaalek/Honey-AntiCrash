package me.alek.honey.checks.listeners;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordIntPair;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.services.ViolationService;
import me.alek.honey.utility.Locations;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PositionListener<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK
        );
    }

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return !Locations.compare3DLocation(packetContainer.getPacket().getLocation(), player.getLocation());
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new PositionA<>());
        visitor.visit(new PositionB<>());
        visitor.visit(new PositionC<>());
        visitor.visit(new PositionD<>());
        visitor.visit(new PositionE<>());
        return visitor;
    }

    @Override
    public void cleanup(Player player) {
        PositionListener.PositionD.lastChunkMap.remove(player.getUniqueId());
        PositionListener.PositionB.lastPacketLocation.remove(player.getUniqueId());
    }

    public static boolean isChunkLoaded(Location location) {
        int x = location.getBlockX() >> 4;
        int z = location.getBlockZ() >> 4;

        return location.getWorld().isChunkLoaded(x, z);
    }

    public static synchronized WrappedChunkCoordIntPair getChunkAt(Location location) {
        if (isChunkLoaded(location)) {
            int x = location.getBlockX() >> 4;
            int z = location.getBlockZ() >> 4;

            return new WrappedChunkCoordIntPair(x, z);
        }
        return null;
    }

    public static class PositionA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                final double x = packet.getX();
                final double y = packet.getY();
                final double z = packet.getZ();

                if (x > 3.0E7 || x < -3.0E7 || z > 3.0E7 || z < -3.0E7 || y < ConfigurationOptions.Position.PositionA.MIN || y > ConfigurationOptions.Position.PositionA.MAX) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Position.A";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionA.class);
        }
    }

    public static class PositionB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, Location> lastPacketLocation = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                try {
                    final double maxDistance = ConfigurationOptions.Position.PositionB.MAX;
                    final double minDistance = ConfigurationOptions.Position.PositionB.MIN;

                    final Location playerLoc = player.getLocation();
                    final Location packetLoc = packet.getLocation();
                    final Location lastKnownPacketLoc = lastPacketLocation.get(player.getUniqueId());

                    if (lastKnownPacketLoc != null) {

                        // for at undgå flag ved lagspikes
                        final double distance2d_client = Locations.calculate2DDistance(packetLoc, lastKnownPacketLoc);
                        final double distanceY_client = Locations.calculateYDistance(packetLoc, lastKnownPacketLoc);

                        if ((distance2d_client == 0) || distance2d_client < maxDistance && distance2d_client > minDistance) {
                            return ResponseBuilder.SUCCESS;
                        }
                        if ((distanceY_client == 0) || distanceY_client < maxDistance && distanceY_client > minDistance) {
                            return ResponseBuilder.SUCCESS;
                        }
                    }
                    lastPacketLocation.put(player.getUniqueId(), packetLoc);

                    final double distance2d = Locations.calculate2DDistance(packetLoc, playerLoc);
                    final double distanceY = Locations.calculateYDistance(packetLoc, playerLoc);

                    if (distance2d > maxDistance || distance2d < minDistance && distance2d != 0) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }
                    if (distanceY > maxDistance || distanceY < minDistance && distanceY != 0) {
                        return ResponseBuilder.inst().cancel().flag().build();
                    }
                } catch (Exception ex) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Position.B";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionB.class);
        }
    }

    public static class PositionC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                if (!player.getAllowFlight()) {

                    // ignorerer når man falder ned
                    if (player.getLocation().getY() - packet.getLocation().getY() > 0) {
                        return ResponseBuilder.SUCCESS;
                    }

                    double distance = Locations.calculate3DDistance(player.getLocation(), packet.getLocation());
                    return ResponseBuilder.inst().setFlagged(distance).onViolation(() -> packet.getContainer().cancel()).withKey(new ViolationService.CheckKey(this, "PositionC")).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Position.C";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionC.class);
        }
    }

    public static class PositionD<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, WrappedChunkCoordIntPair> lastChunkMap = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                WrappedChunkCoordIntPair chunk = getChunkAt(packet.getLocation());

                ResponseContainer response = ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
                if (chunk != null) {
                    if (lastChunkMap.containsKey(player.getUniqueId()) && lastChunkMap.get(player.getUniqueId()).equals(chunk)) {
                        return ResponseBuilder.SUCCESS.builder().terminate().build(); // skipper unloaded chunk check
                    }
                    lastChunkMap.put(player.getUniqueId(), chunk);
                    return response.builder().terminate().build();
                }
                return response;
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Position.D";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionD.class);
        }
    }

    public static class PositionE<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().cancel().flag().build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Position.E";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionE.class);
        }
    }

    // PositionF

    // interval, percentage-of-vertical-switch, vls-amount

    // tjekker seneste cached model class "VelocityReference", og sammenligner den direction med
    // den direction som bliver regnet ud ift. til seneste lokation af den VelocityReference
    // hvis det er en anden direction end den tidligere cached, vil det blive flagged som target,
    // ellers vil det blive flagged som normal (forestil dig hvis du hopper, vil alle position packets
    // fra jorden op til det højeste punkt være i et flow af samme direction, men hvis man derimod
    // spammer en masse packets op og ned i et mønster der ikke er normalt i Minecraft, vil det blive
    // flagged som target. Når det interval er ovre, tjekker den om target er højere end det antal af
    // procent som er blevet konfigureret til. jeg tænkte på omkring min. 35-45

    // put det ind i violation service og lav det som en mulighed for andre modules også, hvis jeg tænker
    // at det kunne være en god idé at implementere flere steder i fremtiden. Det samme gælder for
    // buffer violation check, jeg har bare ikke noget konkret sted lige nu at bruge det, men jeg ved det
    // vil blive nyttigt på et tidspunkt

}
