package me.alek.honey.checks.impl;

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
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordIntPair;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.common.WrappedAbstractPositional;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutPosition;
import me.alek.honey.services.ViolationService;
import me.alek.honey.utility.Locations;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PositionListener<WP extends WrappedAbstractPositional<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final Map<UUID, SampleDecayableList<Location>> lastTeleportationLocationMap = new HashMap<>();

    public PositionListener() {
        HoneyPlugin.get().getPacketLibrary().addListener(new SyncPacketAdapter<WrappedPlayOutPosition>() {

            @Override
            public void onPacketSend(Player player, PacketContainer<WrappedPlayOutPosition> packetContainer) {
                synchronized (lastTeleportationLocationMap) {
                    UUID uuid = player.getUniqueId();

                    if (!lastTeleportationLocationMap.containsKey(uuid)) lastTeleportationLocationMap.put(uuid, new SampleDecayableList<>(20000, 10));
                    SampleDecayableList<Location> sampleDecayableList = lastTeleportationLocationMap.get(uuid);

                    sampleDecayableList.put(packetContainer.getPacket().getLocation());
                }
            }

        }, ListenerPriority.HIGHEST, () -> PacketType.Play.Server.POSITION);
    }

    public static class SampleDecayableList<T> {

        private final long collectInterval;
        private final int sampleSize;
        private final LinkedList<SampleNode> sampleNodes = new LinkedList<>();

        public SampleDecayableList(long collectInterval, int sampleSize) {
            this.collectInterval = collectInterval;
            this.sampleSize = sampleSize;
        }

        public class SampleNode {

            public SampleNode(T value) { this.value = value; }

            private T value;
            private long insertionInstant = System.currentTimeMillis();

        }

        public void put(T node) {
            synchronized (sampleNodes) {
                sampleNodes.addLast(new SampleNode(node));

                if (sampleNodes.size() > sampleSize) sampleNodes.removeFirst();
            }
        }

        private void collect() {
            synchronized (sampleNodes) {

                Iterator<SampleNode> sampleNodeIterator = sampleNodes.iterator();
                while (sampleNodeIterator.hasNext()) {

                    SampleNode node = sampleNodeIterator.next();
                    long diff = System.currentTimeMillis() - node.insertionInstant;

                    if (diff > collectInterval) {
                        sampleNodeIterator.remove();
                        continue;
                    }
                    break;
                }
            }
        }

        private T getWithoutCollecting(int index) {
            synchronized (sampleNodes) {
                return sampleNodes.get(index).value;
            }
        }

        public T get(int index) {
            collect();
            synchronized (sampleNodes) {
                return sampleNodes.get(index).value;
            }
        }

        public T getLast() {
            return get(sampleNodes.size());
        }

        public T getFirst() {
            return get(0);
        }

        public List<T> getAll() {
            collect();
            List<T> list = new ArrayList<>();

            for (int i = 0; i < sampleNodes.size(); i++) {
                list.add(getWithoutCollecting(i));
            }

            return list;
        }
    }

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
        PositionD.manipulatedFlightSettingsMap.remove(player.getUniqueId());
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

    public static boolean isAnyLatelyTeleportationLocationsCloseTo(Player player, Location currentLocation, double diff) {
        final UUID uuid = player.getUniqueId();

        synchronized (lastTeleportationLocationMap) {

            if (lastTeleportationLocationMap.containsKey(uuid)) {

                SampleDecayableList<Location> sampleDecayableList = lastTeleportationLocationMap.get(uuid);
                for (Location location : sampleDecayableList.getAll()) {

                    double locationDiff = Locations.calculate3DDistance(location, currentLocation);
                    if (locationDiff < diff) {
                        return true;
                    }
                }
            }
        }
        return false;
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

                double maxDistance = ConfigurationOptions.Position.PositionB.MAX;
                double minDistance = ConfigurationOptions.Position.PositionB.MIN;

                final Location packetLoc = packet.getLocation();
                final Location lastKnownPacketLoc = lastPacketLocation.getOrDefault(player.getUniqueId(), packetLoc);
                container.putLocation(lastKnownPacketLoc);

                lastPacketLocation.put(player.getUniqueId(), packetLoc);

                if (isAnyLatelyTeleportationLocationsCloseTo(player, packet.getLocation(), 5)) {
                    container.putBoolean(true);
                    return ResponseBuilder.SUCCESS;
                }

                // 2d distance check
                final double distance2d = Locations.calculate2DDistance(packetLoc, lastKnownPacketLoc);
                if (distance2d >= maxDistance || distance2d <= minDistance && distance2d != 0) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }

                // y distance check
                final double distanceY = Locations.calculateYDistance(packetLoc, lastKnownPacketLoc);
                if (distanceY >= maxDistance || distanceY <= minDistance && distanceY != 0) {
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

    public static class PositionC<WP extends WrappedAbstractPositional<WP>> implements PacketCheck<WP>, ViolationCheck {

        private final Map<UUID, Location> lastFlaggedLocationMap = new HashMap<>();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                // tjekker om en spiller flyver, for så kan man sagtens fylde bufferen hurtigt op alligevel
                if (player.isFlying()) {
                    return ResponseBuilder.SUCCESS;
                }

                UUID uuid = player.getUniqueId();

                // ignorerer når man falder ned. bemærk, at hvis den havde været på 0, vil den også ignorere
                // når man går rundt på jorden, da tyngdekraft er en stor faktor ift. en spillers velocity
                if (player.getVelocity().getY() < -1.5) {
                    return ResponseBuilder.SUCCESS;
                }

                Location priorLocation = container.getLocation(0);
                // returnerer hvis det er den første lokation packet der bliver sendt
                if (priorLocation == null) {
                    return ResponseBuilder.SUCCESS;
                }

                // ignorerer packets tæt på den seneste teleportering, da distancen mellem to
                // position packets ellers kan være rigtig høj hvis man teleporterer en lang distance
                Boolean shouldIgnore = container.getBoolean(0);
                if (shouldIgnore != null && shouldIgnore) {
                    return ResponseBuilder.SUCCESS;
                }

                // ignorerer packets tæt på den seneste flag for at minimere false positives ved lagspikes
                if (lastFlaggedLocationMap.containsKey(uuid)) {

                    if (Locations.calculate3DDistance(lastFlaggedLocationMap.get(uuid), packet.getLocation()) < 1.5) {
                        return ResponseBuilder.SUCCESS;
                    }
                }
                lastFlaggedLocationMap.put(player.getUniqueId(), packet.getLocation());

                double priorDistance = Locations.calculate3DDistance(priorLocation, packet.getLocation());
                if (HoneyPlugin.get().getLoggerService().isDebug()) {
                    HoneyPlugin.get().getLogger().info("DEBUG: position distance buffer inc: " + priorDistance);
                }
                // this might be a false positive, shouldn't be that high. but position B would flag for this otherwise
                if (priorDistance > 20) return ResponseBuilder.SUCCESS;

                return ResponseBuilder.inst().setFlagged(priorDistance).onViolation(() -> packet.getContainer().cancel()).withKey(new ViolationService.CheckKey(this, "PositionC")).build();
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
        private static final Set<UUID> manipulatedFlightSettingsMap = new HashSet<>();

        private final ViolationService.CheckKey flightKey = new ViolationService.CheckKey("Position.B", true);
        private final ViolationService.CheckKey nonFlightKey = new ViolationService.CheckKey("Position.B", false);

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ViolationService.CheckKey key = (player.isFlying()) ? flightKey : nonFlightKey;

                // laver en kopi af settings for at lave en til når man flyver, da man kan besøge langt flere chunks så
                if (!manipulatedFlightSettingsMap.contains(player.getUniqueId())) {
                    int maxPerInterval = getSetting().getMaxPerInterval();
                    ConfigurationOptions.VlSetting setting = getSetting().clone();
                    setting.MAX_PER_INTERVAL = maxPerInterval * 3;

                    // tvinger en ny packet limit handler til at blive oprettet med den nye flight setting
                    HoneyPlugin.get().getViolationService().getLimits(player, flightKey, setting);

                    manipulatedFlightSettingsMap.add(player.getUniqueId());
                }
                WrappedChunkCoordIntPair chunk = getChunkAt(packet.getLocation());

                ResponseContainer response = ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
                if (chunk != null) {
                    if (lastChunkMap.containsKey(player.getUniqueId()) && lastChunkMap.get(player.getUniqueId()).equals(chunk)) {
                        return ResponseBuilder.SUCCESS.builder().skip(1).build(); // skipper unloaded chunk check
                    }
                    lastChunkMap.put(player.getUniqueId(), chunk);
                    return response.builder().withKey(key).skip(1).build();
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
                if (priorLocation == null)
                    return ResponseBuilder.SUCCESS;

                Direction[] velocity = calculateVelocity(priorLocation, packet.getLocation());
                Direction[] priorVelocity = lastDirectionFlowMap.get(player.getUniqueId());

                lastDirectionFlowMap.put(player.getUniqueId(), velocity);
                if (priorVelocity != null && priorVelocity.length == velocity.length) {

                    int i = -1;
                    for (Direction dir : velocity) {

                        // sørger for der ikke sker uoverensstemmelse i længderne og indices af de forskellige velocities
                        if (++i == priorVelocity.length)
                            break;

                        Axis axis = axisLookup.get(dir);
                        Direction corr = priorVelocity[i];

                        if (corr == Direction.STILL || dir == Direction.STILL)
                            continue;

                        if (corr == axis.getOther(dir)) {
                            HoneyPlugin.get().getViolationService().flag(player, packet.getHandle(), new ViolationService.CheckKey("Position.F", axis.ordinal()), 1, () -> packet.getContainer().cancel(), getViolationName(), getSetting());
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
