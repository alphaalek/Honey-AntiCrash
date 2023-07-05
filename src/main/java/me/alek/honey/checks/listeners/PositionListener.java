package me.alek.honey.checks.listeners;

import me.alek.honey.HoneyPlugin;
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
import me.alek.honey.netty.packetwrappers.play.client.common.WrappedAbstractPositional;
import me.alek.honey.services.ViolationService;
import me.alek.honey.utility.Locations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PositionListener<WP extends WrappedAbstractPositional<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.VEHICLE_MOVE
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
        visitor.visit(new PositionF<>());
        visitor.visit(new PositionG<>());
        return visitor;
    }

    @Override
    public void cleanup(Player player) {
        PositionB.lastPacketLocation.remove(player.getUniqueId());
        PositionD.lastChunkMap.remove(player.getUniqueId());
        PositionF.lastDirectionFlowMap.remove(player.getUniqueId());
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

    public static class PositionA<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

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

    public static class PositionB<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, Location> lastPacketLocation = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                try {
                    final double maxDistance = ConfigurationOptions.Position.PositionB.MAX;
                    final double minDistance = ConfigurationOptions.Position.PositionB.MIN;

                    final Location playerLoc = player.getLocation();
                    final Location packetLoc = packet.getLocation();
                    final Location lastKnownPacketLoc = lastPacketLocation.getOrDefault(player.getUniqueId(), packetLoc);
                    container.putLocation(lastKnownPacketLoc);

                    lastPacketLocation.put(player.getUniqueId(), packetLoc);

                    // tjekker om den lokation tilhørende en packet er tæt på den lokation man teleporterede væk fra
                    // hvis ikke man gør det, kan der komme false flags hvis clientside lagger og bliver teleporteret serverside
                    if (PositionC.shouldIgnore(player, packet.getLocation())) {
                        return ResponseBuilder.SUCCESS;
                    }

                    // 2d distance check
                    final double distance2d_client = Locations.calculate2DDistance(packetLoc, lastKnownPacketLoc);
                    final double distance2d = Locations.calculate2DDistance(packetLoc, playerLoc);

                    if (distance2d_client > maxDistance || distance2d_client < minDistance) {
                        if ((distance2d > maxDistance || distance2d < minDistance) && distance2d != 0) {

                            return ResponseBuilder.inst().cancel().flag().build();
                        }
                    }

                    // y distance check
                    final double distanceY_client = Locations.calculateYDistance(packetLoc, lastKnownPacketLoc);
                    final double distanceY = Locations.calculateYDistance(packetLoc, playerLoc);

                    if (distanceY_client > maxDistance || distanceY_client < minDistance) {
                        if ((distanceY > maxDistance || distanceY < minDistance) && distanceY != 0) {

                            if (!(distanceY < minDistance && player.isFlying())) {
                                return ResponseBuilder.inst().cancel().flag().build();
                            }
                        }
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

    public static class PositionC<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck, Listener {

        private static final Map<UUID, Long> lastTeleportationInstantMap = new ConcurrentHashMap<>();
        private static final Map<UUID, Location> lastTeleportationMap = new ConcurrentHashMap<>();
        private static final Map<UUID, Long> lastJoinMap = new ConcurrentHashMap<>();

        public PositionC() {
            Bukkit.getPluginManager().registerEvents(this, HoneyPlugin.get());
        }

        @EventHandler
        public void onTeleport(PlayerTeleportEvent event) {
            lastTeleportationInstantMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
            lastTeleportationMap.put(event.getPlayer().getUniqueId(), event.getFrom());
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            lastJoinMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            lastJoinMap.remove(event.getPlayer().getUniqueId());
        }

        public static boolean shouldIgnore(Player player, Location packetLocation) {
            UUID uuid = player.getUniqueId();
            if (lastTeleportationMap.containsKey(uuid)) {

                // tjekker om man lige er joinet
                if (lastJoinMap.containsKey(uuid) && System.currentTimeMillis() - lastJoinMap.get(uuid) < 3000) {
                    return false;
                }
                // tjekker om man ikke er teleporteret inden for det seneste halve minut
                if ((System.currentTimeMillis() - lastTeleportationInstantMap.get(uuid))/1000 > 30) {
                    return false;
                }
                // tjekker om den lokation tilhørende en packet er tæt på den lokation man teleporterede væk fra
                // hvis ikke man gør det, kan der komme false flags hvis clientside lagger og bliver teleporteret serverside
                return Locations.calculate3DDistance(lastTeleportationMap.get(uuid), packetLocation) < 50;
            }
            return false;
        }

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                if (!player.getAllowFlight()) {

                    // ignorerer når man falder ned
                    if (player.getVelocity().getY() < 0) {
                        return ResponseBuilder.SUCCESS;
                    }

                    // ignorer hvis man lige har teleporteret
                    if (shouldIgnore(player, packet.getLocation())) {
                        return ResponseBuilder.SUCCESS;
                    }
                    double distance = Locations.calculate3DDistance(player.getLocation(), packet.getLocation());
                    Bukkit.getLogger().info("distance: " + distance);

                    Location loc = player.getLocation();
                    Bukkit.getLogger().info("player xyz: " + loc.getX() + "," + loc.getY() + "," + loc.getZ() + " - packet xyz: " + packet.getX() + "," + packet.getY() + "," + packet.getZ());

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

    public static class PositionD<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<UUID, WrappedChunkCoordIntPair> lastChunkMap = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {
                WrappedChunkCoordIntPair chunk = getChunkAt(packet.getLocation());

                ResponseContainer response = ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
                if (chunk != null) {
                    if (lastChunkMap.containsKey(player.getUniqueId()) && lastChunkMap.get(player.getUniqueId()).equals(chunk)) {
                        return ResponseBuilder.SUCCESS.builder().skip(1).build(); // skipper unloaded chunk check
                    }
                    lastChunkMap.put(player.getUniqueId(), chunk);
                    return response.builder().skip(1).build();
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

    public static class PositionE<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

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

    public static class PositionF<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private static final Map<Direction, Axis> axisLookup = new EnumMap<>(Direction.class);

        private enum Direction {
            UP, DOWN, NORTH, SOUTH, WEST, EAST, STILL
        }

        private enum Axis {
            HORIZONTAL_1(Direction.NORTH, Direction.SOUTH),
            HORIZONTAL_2(Direction.WEST, Direction.EAST),
            VERTICAL(Direction.UP, Direction.DOWN);

            private final Direction opp1;
            private final Direction opp2;

            public Direction getOther(Direction dir) {
                return (dir == opp1) ? opp2 : opp1;
            }

            Axis(Direction opp1, Direction opp2) {
                this.opp1 = opp1;
                this.opp2 = opp2;
            }
        }

        static {

            for (Axis axis : Axis.values()) {
                axisLookup.put(axis.opp1, axis);
                axisLookup.put(axis.opp2, axis);
            }
        }

        private Direction[] calculateVelocity(Location loc1, Location loc2) {
            Direction[] velocity = new Direction[3];

            double deltaX = loc1.getX() - loc2.getX();
            velocity[0] = (deltaX > 0) ? Direction.EAST : (deltaX == 0) ? Direction.STILL : Direction.WEST;

            double deltaY = loc1.getY() - loc2.getY();
            velocity[1] = (deltaY > 0) ? Direction.DOWN : (deltaY == 0) ? Direction.STILL : Direction.UP;

            double deltaZ = loc1.getZ() - loc2.getZ();
            velocity[2] = (deltaZ > 0) ? Direction.SOUTH : (deltaZ == 0) ? Direction.STILL : Direction.NORTH;

            return velocity;
        }

        private static final Map<UUID, Direction[]> lastDirectionFlowMap = new ConcurrentHashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                Location priorLocation = container.getLocation(0);
                // når den allerførste lokation packet bliver sendt, er der ikke nogen prior location, og derfor skal den
                // bare returnere indtil den næste position packet kommer
                if (priorLocation == null) {
                    return ResponseBuilder.SUCCESS;
                }

                Direction[] velocity = calculateVelocity(priorLocation, packet.getLocation());
                Direction[] priorVelocity = lastDirectionFlowMap.get(player.getUniqueId());

                lastDirectionFlowMap.put(player.getUniqueId(), velocity);
                if (priorVelocity != null && priorVelocity.length == velocity.length) {

                    int i = -1;
                    for (Direction dir : velocity) {

                        // sørger for der ikke sker uoverensstemmelse i længderne af de forskellige velocities
                        if (++i == priorVelocity.length) {
                            break;
                        }
                        Axis axis = axisLookup.get(dir);
                        Direction corr = priorVelocity[i];

                        if (corr == Direction.STILL || dir == Direction.STILL) continue;

                        if (corr == axis.getOther(dir)) {
                            HoneyPlugin.get().getViolationService().flag(player, packet.getHandle(), new ViolationService.CheckKey(this, axis), 1, () -> packet.getContainer().cancel(), getViolationName(), getSetting());
                        }
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Position.F";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionF.class);
        }
    }

    public static class PositionG<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                // tjekker om den packet er en vehicle move packet
                if (packet.getType() == PacketType.Play.Client.VEHICLE_MOVE) {

                    if (player.getVehicle() == null) {
                        return ResponseBuilder.inst().flag().onViolation(() -> packet.getContainer().cancel()).build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Position.G";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Position.PositionG.class);
        }
    }
}
