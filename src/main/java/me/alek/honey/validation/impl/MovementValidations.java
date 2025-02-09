package me.alek.honey.validation.impl;

import me.alek.honey.HoneyKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordinates;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.MovementPacketWrapper;
import me.alek.honey.netty.packetwrappers.impl.play.server.WrappedPlayOutPosition;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.util.Callback;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.statics.LocationUtils;
import me.alek.honey.validation.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;
import java.util.function.Function;

public class MovementValidations<WP extends MovementPacketWrapper<WP>> extends AbstractValidationPacketListener<WP> {

    private static final Location ORIGIN_LOCATION = WrappedBlockPosition.ORIGIN.toLocation(null);

    public static MovementValidations<?> INSTANCE;
    private final Map<UUID, Location> lastTeleportationLocationMap = new HashMap<>();
    private final Map<UUID, Location> lastPacketLocation = new HashMap<>();

    public MovementValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.NORMAL, getPacketTypes());
        INSTANCE = this;
        HoneyPlugin.get().getPacketLibrary().addListener(new PacketListenerAdapter.InboundCommon<WrappedPlayOutPosition>() {

            @Override
            public void onPacketReceive(HoneyPlayer player, PacketContainer<WrappedPlayOutPosition> packet) {
                lastTeleportationLocationMap.put(player.uuid, packet.getPlayer().getLocation());
            }

        }, PacketContainer.PipelineState.OBJECT, ListenerPriority.HIGHEST, PacketTypes.Play.Server.PLAYER_POSITION);
    }

    private static List<PacketType> getPacketTypes() {
        return Arrays.asList(
                PacketTypes.Play.Client.PLAYER_POSITION,
                PacketTypes.Play.Client.PLAYER_POS_ROT,
                PacketTypes.Play.Client.GROUND,
                PacketTypes.Play.Client.PLAYER_MOVEMENT
        );
    }

    public static String format(double value) {
        return String.valueOf(Math.ceil(value * 100) / 100);
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext context) {
        Location currentLoc = packet.getCurrentWrapped().getLocation(player.getWorld());

        if (LocationUtils.compareLocation(currentLoc, ORIGIN_LOCATION)) {
            return false;
        }
        this.lastPacketLocation.put(player.uuid, currentLoc);

        context.getCallback().addAction(Callback.Action.<ViolationKey, ValidationViolationContext>from((keyOuter, response) -> {
            response.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                if (onViolation != null) {
                    onViolation.run();
                }

                final String x = format(currentLoc.getX());
                final String y = format(currentLoc.getY());
                final String z = format(currentLoc.getZ());

                context.getDevelopmentData(keyInner).addPoint("Position", x + "," + y + "," + z);
            });
        }));

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValidationPipeline<PacketValidation<WP>> createValidationPipeline() {
        return ValidationPipeline.from(new MovementA<>(), new MovementB<>(), new MovementC<>(), new MovementD<>(), new MovementE<>(),
                new MovementF<>(), new MovementG<>(), new MovementH<>(), new MovementI<>(), new MovementJ<>());
    }

    @Override
    public void cleanup(HoneyPlayer player) {
        this.lastPacketLocation.remove(player.uuid);
        this.lastTeleportationLocationMap.remove(player.uuid);
    }

    public Location getLastLocationFor(HoneyPlayer player) {
        return this.lastPacketLocation.getOrDefault(player.uuid, player.getLocation());
    }

    public static boolean isChunkLoaded(Location location) {
        int x = location.getBlockX() >> 4;
        int z = location.getBlockZ() >> 4;

        return location.getWorld().isChunkLoaded(x, z);
    }

    public static boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    public static synchronized WrappedChunkCoordinates getChunkAt(Location location) {
        int x = ((int) location.getX()) >> 4;
        int z = ((int) location.getZ()) >> 4;

        return new WrappedChunkCoordinates(x, z, world -> isChunkLoaded(world, x, z));
    }

    public boolean isAnyLatelyTeleportationLocationsCloseTo(HoneyPlayer player, double diff, Function<Location, Double> distanceFunction) {
        if (this.lastTeleportationLocationMap.containsKey(player.uuid)) {
            Location lastTeleportationLocation = this.lastTeleportationLocationMap.get(player.uuid);
            double distance = distanceFunction.apply(lastTeleportationLocation);

            return distance != -1 && distance < diff;
        }

        return false;
    }

    public static class MovementA<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private boolean wasOnGround;

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (packet.getType() == PacketTypes.Play.Client.PLAYER_POSITION || packet.getType() == PacketTypes.Play.Client.PLAYER_POS_ROT) {
                return ValidationViolationContext.PASS;
            }

            if (getSetting().isEnabled()) {
                boolean wasOnGround = this.wasOnGround;
                this.wasOnGround = packet.isOnGround();
                if (wasOnGround != this.wasOnGround) {
                    return ValidationViolationContext.Builder.validate().terminate().flag().build();
                }
            }

            return ValidationViolationContext.Builder.validate().terminate().build();
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementA.class);
        }
    }

    public static class MovementB<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private static final HoneyKey XZKey = HoneyKey.of(0, "2d");
        private static final HoneyKey YKey = HoneyKey.of(1, "y");

        private Location lastLoc;

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            Location loc = packet.getLocation(player.getWorld());
            Location lastLoc = Optional.ofNullable(this.lastLoc).orElse(loc);
            this.lastLoc = loc;

            if (INSTANCE.isAnyLatelyTeleportationLocationsCloseTo(
                    player, getSetting().max,
                    l -> LocationUtils.calculate3DDistance(l, loc))) {
                return ValidationViolationContext.PASS.copy().skip(3).build();
            }

            double distance2d = LocationUtils.calculate2DDistance(loc, lastLoc);
            context.putDouble(distance2d);

            double distanceY = LocationUtils.calculateYDistance(loc, lastLoc);
            context.putDouble(distanceY);

            if (getSetting().isEnabled()) {
                ValidationViolationContext.Builder builder = ValidationViolationContext.Builder.validate();

                if (distance2d >= getSetting().max && distance2d != 0) {
                    String x = format(lastLoc.getX());
                    String y = format(lastLoc.getY());
                    String z = format(lastLoc.getZ());

                    builder.add(
                            ValidationViolationContext.Builder.validate().cancel().flag().withViolationKey(XZKey)
                                    .onViolation(ViolationKey.of(this, XZKey), (key, onViolation) -> {
                                        if (onViolation != null) {
                                            onViolation.run();
                                        }
                                        context.getDevelopmentData(key).addPoint("Last Position", x + "," + y + "," + z);
                                        context.getDevelopmentData(key).addPoint("2D Distance", distance2d);
                                    })
                                    .build()
                    );
                }

                if (distanceY >= getSetting().max && distanceY != 0) {
                    String x = format(lastLoc.getX());
                    String y = format(lastLoc.getY());
                    String z = format(lastLoc.getZ());

                    builder.add(
                            ValidationViolationContext.Builder.validate().cancel().flag().withViolationKey(YKey)
                                    .onViolation(ViolationKey.of(this, YKey), (key, onViolation) -> {
                                        if (onViolation != null) {
                                            onViolation.run();
                                        }
                                        context.getDevelopmentData(key).addPoint("Last Position", x + "," + y + "," + z);
                                        context.getDevelopmentData(key).addPoint("Y Distance", distanceY);
                                    })
                                    .build()
                    );
                }

                if (!builder.build().subContainers.isEmpty()) {
                    return builder.build();
                }
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.b");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementB.class);
        }
    }

    public static class MovementC<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private Location lastFlaggedLoc;
        private Location lastLoc;

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            Location loc = packet.getLocation(player.getWorld());
            Location lastLoc = Optional.ofNullable(this.lastLoc).orElse(loc);
            this.lastLoc = loc;

            if (getSetting().isEnabled()) {
                // ignores when you fall down. note, that if it would be at 0, it will also ignore when you
                // walk around on ground, because the gravity affect the player's velocity
                if (player.bukkit.getVelocity().getY() < -1.5) {
                    return ValidationViolationContext.PASS;
                }

                // return if it's the first location being sent
                if (lastLoc == null) {
                    return ValidationViolationContext.PASS;
                }

                // ignores packets close to the recent flag
                if (this.lastFlaggedLoc != null
                        && LocationUtils.calculate3DDistance(lastFlaggedLoc, loc) < 1) {
                    return ValidationViolationContext.PASS;
                }
                this.lastFlaggedLoc = loc;

                double priorDistance = LocationUtils.calculate3DDistance(lastLoc, loc);
                return ValidationViolationContext.Builder.validate().flag(Math.min(10, priorDistance)).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.c");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementC.class);
        }
    }

    public static class MovementD<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private WrappedChunkCoordinates lastChunkCameFrom;
        private WrappedChunkCoordinates lastChunk;

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            // gets the chunk of the player's location which should be compared to the last chunk of the player, and this way
            // consider if the player is moving too fast around the map and loading excessively chunks
            WrappedChunkCoordinates chunk = getChunkAt(packet.getLocation(player.getWorld()));
            boolean equalsChunk = chunk.equals(this.lastChunk);
            boolean equalsPreviousChunk = chunk.equals(this.lastChunkCameFrom);

            int skip = equalsChunk || equalsPreviousChunk ? 1 : chunk.isLoaded(player.getWorld()) ? 1 : 0;

            if (!equalsChunk) {
                this.lastChunkCameFrom = this.lastChunk;
                this.lastChunk = chunk;
            }

            // if the setting is disabled, we will just return SUCCESS
            // or if the player is in the same chunk as the last position or the same chunk as the previous chunk
            // the skip is for letting the next check in the check pipeline (Movement.E), either flag or not flag
            if (!getSetting().isEnabled()
                    || equalsChunk
                    || equalsPreviousChunk
                    || this.lastChunkCameFrom == null) {
                return ValidationViolationContext.PASS.copy().skip(skip).build();
            }

            return ValidationViolationContext.Builder.validate().flag().skip(skip).build();
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.D";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.d");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementD.class);
        }
    }

    public static class MovementE<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag().build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.E";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.e");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementE.class);
        }
    }

    public static class MovementF<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        private static final Map<Direction, Axis> AXISES = new EnumMap<>(Direction.class);

        private enum Direction {
            UP, DOWN, NORTH, SOUTH, WEST, EAST, STILL
        }

        private enum Axis {
            X(Direction.WEST, Direction.EAST),
            Z(Direction.NORTH, Direction.SOUTH),
            Y(Direction.UP, Direction.DOWN);

            private final Direction opp1;
            private final Direction opp2;
            private final HoneyKey key = HoneyKey.of(ordinal(), name().toLowerCase());

            public Direction getOther(Direction dir) {
                return (dir == opp1) ? opp2 : opp1;
            }

            public HoneyKey getKey() {
                return key;
            }

            Axis(Direction opp1, Direction opp2) {
                this.opp1 = opp1;
                this.opp2 = opp2;
            }
        }

        static {
            for (Axis axis : Axis.values()) {
                AXISES.put(axis.opp1, axis);
                AXISES.put(axis.opp2, axis);
            }
        }

        private Direction[] calculateVelocity(Location loc1, Location loc2) {
            final Direction[] velocity = new Direction[3];

            final double deltaX = loc1.getX() - loc2.getX();
            velocity[0] = (deltaX > 0) ? Direction.EAST : (deltaX == 0) ? Direction.STILL : Direction.WEST;

            final double deltaY = loc1.getY() - loc2.getY();
            velocity[1] = (deltaY > 0) ? Direction.DOWN : (deltaY == 0) ? Direction.STILL : Direction.UP;

            final double deltaZ = loc1.getZ() - loc2.getZ();
            velocity[2] = (deltaZ > 0) ? Direction.SOUTH : (deltaZ == 0) ? Direction.STILL : Direction.NORTH;

            return velocity;
        }

        private Location lastLoc;
        private Direction[] lastDirectionFlow;

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            Location loc = packet.getLocation(player.getWorld());
            Location lastLoc = Optional.ofNullable(this.lastLoc).orElse(loc);
            this.lastLoc = loc;

            if (getSetting().isEnabled()) {
                if (LocationUtils.compare3DLocation(loc, lastLoc)) {
                    return ValidationViolationContext.PASS;
                }

                Direction[] velocity = calculateVelocity(lastLoc, loc);
                Direction[] priorVelocity = lastDirectionFlow;
                this.lastDirectionFlow = velocity;
                if (priorVelocity == null) {
                    return ValidationViolationContext.PASS;
                }

                ValidationViolationContext.Builder builder = ValidationViolationContext.Builder.validate();
                for (int i = 0; i < 3; i++) {
                    Direction dir = velocity[i];
                    Direction corr = priorVelocity[i];
                    if (corr == Direction.STILL || dir == Direction.STILL) {
                        continue;
                    }

                    Axis axis = AXISES.get(dir);
                    if (corr.equals(axis.getOther(dir))) {
                        builder.add(
                                ValidationViolationContext.Builder.validate().flag()
                                        .withViolationKey(axis.getKey())
                                        .build()
                        );
                    }
                }

                if (!builder.build().subContainers.isEmpty()) {
                    return builder.build();
                }
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.F";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.f");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementF.class);
        }
    }

    public static class MovementG<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.G";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.g");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementG.class);
        }
    }

    public static class MovementH<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP>{

        private static final int id = IDRegistry.VALIDATION.acquire();
        private static final HoneyKey twoDKey = HoneyKey.of(0, "2d");
        private static final HoneyKey YKey = HoneyKey.of(1, "y");

        private double lastDelta2D;
        private double lastDeltaY;

        @Override
        public synchronized ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                double delta2D = context.getDouble(0);
                double deltaY = context.getDouble(1);
                if (delta2D == 0 && deltaY == 0) {
                    return ValidationViolationContext.PASS;
                }
                if (player.isInLiquid() || deltaY % 0.375 < Math.pow(10, -3)) {
                    return ValidationViolationContext.PASS;
                }

                ValidationViolationContext.Builder builder = ValidationViolationContext.Builder.validate();

                if (this.lastDeltaY == deltaY && deltaY > 0) {
                    builder.add(
                            context.put(ValidationViolationContext.class,
                                    ValidationViolationContext.Builder.validate().flag()
                                            .withViolationKey(YKey)
                                            .build()
                            )
                    );
                }
                if (this.lastDelta2D == delta2D && delta2D > 0) {
                    builder.add(
                            context.put(ValidationViolationContext.class,
                                    ValidationViolationContext.Builder.validate().flag()
                                            .withViolationKey(twoDKey)
                                            .build()
                            )
                    );
                }

                this.lastDeltaY = deltaY;
                this.lastDelta2D = delta2D;

                return builder.build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.H";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.h");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementH.class);
        }
    }

    public static class MovementI<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                ValidationViolationContext.Builder builder = ValidationViolationContext.Builder.validate();
                int i = 0;
                for (;;) {
                    ValidationViolationContext flagContext = context.get(ValidationViolationContext.class, i++);
                    if (flagContext != null) {
                        builder.add(flagContext);
                        continue;
                    }

                    if (builder.build().subContainers.isEmpty()) {
                        return ValidationViolationContext.PASS;
                    }

                    return builder.build();
                }
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.I";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.i");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementI.class);
        }
    }

    public static class MovementJ<WP extends MovementPacketWrapper<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                Location loc = packet.getLocation(player.getWorld());
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();

                if (x > 3.0E7
                        || x < -3.0E7
                        || z > 3.0E7
                        || z < -3.0E7) {
                    return ValidationViolationContext.Builder.validate().cancel().flag().build();
                }

                if (y < getSetting().min || y > getSetting().max) {
                    if (player.bukkit.isDead()) {
                        return ValidationViolationContext.Builder.validate().cancel().build();
                    }

                    return ValidationViolationContext.Builder.validate().cancel().flag().onPunish(() -> {
                        double yPosFix = y;
                        if (y < getSetting().min) {
                            while (player.getWorld().getBlockAt((int) x, (int) yPosFix++, (int) z).getType() != Material.AIR || yPosFix < 0) {
                                if (yPosFix > getSetting().max - 3) break;
                            }
                        }
                        if (y > getSetting().max) {
                            while (player.getWorld().getBlockAt((int) x, (int) yPosFix--, (int) z).getType() == Material.AIR) {
                                if (yPosFix < 16) break;
                            }
                            yPosFix += 2;
                        }
                        player.bukkit.teleport(new Location(player.getWorld(), x, yPosFix, z));
                    }).build();
                }
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Movement.J";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.movement.j");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Movement.MovementJ.class);
        }
    }
}
