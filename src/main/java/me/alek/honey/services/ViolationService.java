package me.alek.honey.services;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.PacketLimitHandlerType;
import me.alek.honey.netty.UnsafeChannelCloser;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.util.Array;
import me.alek.honey.util.discord.DiscordWebhook;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.types.HoneyValidation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ViolationService {

    private static ViolationService INSTANCE;

    public ViolationService() { INSTANCE = this; }

    public void reload() {
        // loop through all the players and update their packet limit handlers
        for (HoneyPlayer player : HoneyPlayer.getAllPlayers()) {
            player.violationContext = new ViolationContext(player);
        }
    }

    public ViolationDistributor getDistributor(HoneyPlayer player, HoneyKey violationKey, ConfigurationOptions.ViolationSetting setting) {
        return player.violationContext.getDistributor(violationKey, setting);
    }

    public boolean flag(HoneyPlayer player,
                        Cancellable cancellable,
                        HoneyKey distributionKey,
                        HoneyKey violationKey,
                        double amount,
                        Runnable onPunish,
                        Runnable onViolation,
                        HoneyValidation<?> validation,
                        ConfigurationOptions.ViolationSetting setting,
                        ValidationPipelineContext.DevelopmentData developmentData
    ) {
        return getDistributor(player, distributionKey, setting)
                .flag(violationKey, cancellable, validation, amount, onPunish, onViolation, developmentData);
    }

    public boolean flag(HoneyPlayer player,
                        Cancellable cancellable,
                        HoneyKey distributionKey,
                        HoneyKey violationKey,
                        double amount,
                        HoneyValidation<?> validation,
                        ConfigurationOptions.ViolationSetting setting,
                        ValidationPipelineContext.DevelopmentData developmentData
    ) {
        return flag(player, cancellable, distributionKey, violationKey, amount, null, null, validation, setting, developmentData);
    }

    public synchronized void punish(Runnable onPunish, HoneyPlayer player, HoneyValidation<?> validation, HoneyKey violationKey) {
        if (onPunish != null) onPunish.run();

        if (player.punished) return;
        player.punished = true;

        player.channel.eventLoop().execute(() -> {
            if (player.channel.isActive()) {
                UnsafeChannelCloser.closeForcibly(player.channel);
            }
        });

        if (player.isBukkitReady() && player.isOnlineBukkit()) {
            Bukkit.getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> player.bukkit.kickPlayer("Disconnected"));
        }

        if (validation != null && player.isBukkitReady()) {
            HoneyPlugin.get().getNotificationService().warn(player, validation.getName(), violationKey);
        }
    }

    public static class ViolationContext {

        private final HoneyPlayer player;
        private final Array<ViolationDistributor> distributors = new Array<>();

        public ViolationContext(HoneyPlayer player) {
            this.player = player;
        }

        public ViolationDistributor getDistributor(HoneyKey key, ConfigurationOptions.ViolationSetting setting) {
            return distributors.get(key.getRegistry(), () -> {
                if (setting.keyedAnnotation != null) {
                    return new ViolationDistributor(player,
                            new KeyedViolationLookupGenerator(key, setting));
                } else {
                    final AbstractPacketLimitHandler packetLimitHandler = generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
                    if (packetLimitHandler == null) {
                        throw new RuntimeException("No packet limit handler could be created.");
                    }

                    return new ViolationDistributor(player, packetLimitHandler);
                }
            });
        }

        public void setSetting(HoneyKey key, HoneyKey violationKey, ConfigurationOptions.ViolationSetting setting) {
            if (distributors.has(key.getRegistry())) {
                distributors.get(key.getRegistry()).setSetting(violationKey, setting);
            }
        }

        public Array<ViolationDistributor> getViolationDistributors() {
            return distributors;
        }

        public ViolationDistributor getHandler(HoneyKey key) {
            return distributors.get(key.getRegistry(), null);
        }

        public HoneyPlayer getPlayer() {
            return player;
        }
    }

    private static AbstractPacketLimitHandler generatePacketLimitHandler(PacketLimitHandlerType type, HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
        switch (type) {
            case SIMPLE:
                return new PacketSimpleHandler(player, setting);
            case INTERVAL:
                return new PacketIntervalHandler(player, setting);
            case INTERVAL_THROTTLE:
                return new PacketIntervalThrottleHandler(player, setting);
            case SINGLE_OVERFLOW:
                return new PacketSingleOverflowHandler(player, setting);
            case BUFFER_OVERFLOW:
                return new PacketBufferOverflowHandler(player, setting);
            case BUFFER_THROTTLE:
                return new PacketBufferThrottleHandler(player, setting);
            case TRIGGER_THROTTLE:
                return new PacketTriggerThrottleHandler(player, setting);
            case CHIPSTATE_THROTTLE:
                return new PacketChipstateThrottleHandler(player, setting);
            default:
                return null;
        }
    }

    public static class HoneyKey implements Cloneable {

        private final int key;
        private final String identifier;

        private HoneyKey(int key, String identifier) {
            this.key = key;
            this.identifier = identifier;
        }

        public static HoneyKey of(int key) {
            return new HoneyKey(key, null);
        }

        public static HoneyKey of(int key, String identifier) {
            return new HoneyKey(key, identifier);
        }

        public static HoneyKey of(HoneyValidation<?> validation) {
            return of(validation.getID());
        }

        public int getRegistry() {
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
    }

    public static class KeyedViolationLookupGenerator {

        private final ConfigurationOptions.ViolationSetting setting;
        private final HoneyKey limitsKey;

        public KeyedViolationLookupGenerator(HoneyKey limitsKey, ConfigurationOptions.ViolationSetting setting) {
            this.setting = setting;
            this.limitsKey = limitsKey;
        }

        public Map<HoneyKey, AbstractPacketLimitHandler> generateMap(HoneyPlayer player) {
            if (setting.keyedAnnotation == null || setting.keyedAnnotation.amount() <= 0) {
                return new HashMap<HoneyKey, AbstractPacketLimitHandler>(){{
                    put(HoneyKey.of(limitsKey.getRegistry()), generateHandler(player));
                }};
            }

            final Map<HoneyKey, AbstractPacketLimitHandler> map = new HashMap<>();
            for (int i = 0; i < setting.keyedAnnotation.amount(); i++) {
                map.put(HoneyKey.of(i), generateHandler(player));
            }
            return map;
        }

        private AbstractPacketLimitHandler generateHandler(HoneyPlayer player) {
            return generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
        }
    }

    public static class ViolationDistributor {

        private final HoneyPlayer player;
        private final Array<AbstractPacketLimitHandler> packetLimitHandlerDelegateLookup = new Array<>();
        private AbstractPacketLimitHandler cachedDefaultPacketLimitHandler;

        public ViolationDistributor(HoneyPlayer player, KeyedViolationLookupGenerator generator) {
            this.player = player;

            for (Map.Entry<HoneyKey, AbstractPacketLimitHandler> entry : generator.generateMap(player).entrySet()) {
                packetLimitHandlerDelegateLookup.set(entry.getKey().getRegistry(), entry.getValue());
            }
        }

        public ViolationDistributor(HoneyPlayer player, AbstractPacketLimitHandler defaultPacketLimitHandler) {
            this.player = player;
            packetLimitHandlerDelegateLookup.set(0, defaultPacketLimitHandler);
        }

        public AbstractPacketLimitHandler getAnyPacketLimitHandler() {
            if (packetLimitHandlerDelegateLookup.isEmpty()) {
                return null;
            }

            return Arrays.stream(
                    packetLimitHandlerDelegateLookup
                            .compact()
                            .toRealArray(AbstractPacketLimitHandler[]::new))
                    .findAny()
                    .orElse(null);
        }

        public HoneyPlayer getPlayer() {
            return this.player;
        }

        public AbstractPacketLimitHandler getPacketLimitHandler(HoneyKey key, boolean orJustAny) {
            AbstractPacketLimitHandler handler = packetLimitHandlerDelegateLookup.get(key.getRegistry());
            if (handler != null) {
                return handler;
            }

            return orJustAny ? getAnyPacketLimitHandler() : null;
        }

        public Array<AbstractPacketLimitHandler> getPacketLimitHandlers() {
            return packetLimitHandlerDelegateLookup;
        }

        private AbstractPacketLimitHandler getHandler(HoneyKey key) {
            if (packetLimitHandlerDelegateLookup.isEmpty()
                    && cachedDefaultPacketLimitHandler == null) {
                return null;
            }

            if (key == null) {
                return cachedDefaultPacketLimitHandler == null
                        ? (cachedDefaultPacketLimitHandler = getAnyPacketLimitHandler())
                        : cachedDefaultPacketLimitHandler;
            }

            return getPacketLimitHandler(key, false);
        }

        public AbstractPacketLimitHandler createHandler(HoneyKey key, ConfigurationOptions.ViolationSetting setting) {
            if (packetLimitHandlerDelegateLookup.has(key.getRegistry())) {
                return null;
            }

            AbstractPacketLimitHandler handler = generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
            packetLimitHandlerDelegateLookup.set(key.getRegistry(), handler);
            return handler;
        }

        public void setSetting(HoneyKey key, ConfigurationOptions.ViolationSetting setting) {
            AbstractPacketLimitHandler handler = getHandler(key);
            if (handler == null) {
                return;
            }

            handler.setting = setting;
        }

        public boolean flag(HoneyKey key, Cancellable cancellable,
                            HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation,
                            ValidationPipelineContext.DevelopmentData developmentData) {
            AbstractPacketLimitHandler handler = getHandler(key);
            if (handler == null) {
                return false;
            }

            return handler.flag(cancellable, validation, amount, onPunish, onViolation, developmentData, key);
        }
    }

    public static abstract class AbstractPacketLimitHandler {

        private final HoneyPlayer player;
        private ConfigurationOptions.ViolationSetting setting;

        private long lastViolationClear;
        private int violations;

        public AbstractPacketLimitHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            this.player = player;
            this.setting = setting;

            this.lastViolationClear = System.currentTimeMillis();
        }

        public HoneyPlayer getPlayer() {
            return player;
        }

        public ConfigurationOptions.ViolationSetting getSetting() {
            return setting;
        }

        public void doFormatStuff(HoneyValidation<?> validation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (validation.isDefaultPipeline()) {
                HoneyPlugin.get().getNotificationService().applyQuotients(player, developmentData);
                applySettingsToDevelopmentData(developmentData);
            }

            HoneyPlugin.get().getNotificationService().notify(
                    player, validation.getName(), violationKey, getSetting(), this, developmentData);

            if (DiscordWebhook.enabled()) {
                DiscordWebhook.violation(
                        player, validation,
                        ChatColor.stripColor(HoneyPlugin.get().getNotificationService().formatNotification(setting, AbstractPacketLimitHandler.this)),
                        developmentData);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        HoneyPlugin.get().getSQLManager().postViolation(player,
                                validation.getName() + (violationKey != null && violationKey.getIdentifier() != null ? " (" + violationKey.getIdentifier() + ")" : ""),
                                ChatColor.stripColor(HoneyPlugin.get().getNotificationService().formatNotification(setting, AbstractPacketLimitHandler.this)), developmentData);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(HoneyPlugin.get().getPlugin());
        }

        public boolean violate(Cancellable cancellable, HoneyValidation<?> validation, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (System.currentTimeMillis() - lastViolationClear >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violations = 0;
                lastViolationClear = System.currentTimeMillis();
            }

            if (onViolation != null) {
                onViolation.run();
            }
            cancellable.cancel();

            if (++violations >= setting.minVls) {
                doFormatStuff(validation, developmentData, violationKey);
            }

            if (violations < getSetting().vlsAmount) {
                return false;
            }

            INSTANCE.punish(onPunish, player, validation, violationKey);
            return true;
        }

        private void applySettingsToDevelopmentData(ValidationPipelineContext.DevelopmentData developmentData) {
            developmentData.addEmpty();

            PacketLimitHandlerType handlerType = setting.getPacketLimitHandlerType();
            developmentData.addHeader("Settings", (handlerType == PacketLimitHandlerType.SIMPLE ? null : setting.getPacketLimitHandlerType().name().toLowerCase().replace("_", " ")));

            for (Map.Entry<String, Object> valueEntry : setting.getValues()) {
                 developmentData.addLine(valueEntry.getKey() + ": " +valueEntry.getValue());
            }
        }

        public int getViolations() {
            return violations;
        }

        public abstract boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey);

        public abstract PacketLimitHandlerType getType();
    }

    public static class PacketChipstateThrottleHandler extends AbstractPacketLimitHandler {

        public PacketChipstateThrottleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (amount == 1) {
                cancellable.cancel();
                return true;
            }

            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.CHIPSTATE_THROTTLE;
        }
    }

    public static class PacketTriggerThrottleHandler extends AbstractPacketLimitHandler {

        private boolean notFlaggedLast;
        private boolean flagged;
        private double lastFlag;

        public PacketTriggerThrottleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (amount < 1) {
                notFlaggedLast = true;
                flagged = false;
            }

            if (amount >= 1 || System.currentTimeMillis() - lastFlag < getSetting().throttleAfter) {
                if (amount >= 1) {
                    if (notFlaggedLast && !flagged) {
                        violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
                        flagged = true;
                    }

                    lastFlag = System.currentTimeMillis();
                    notFlaggedLast = true;
                }

                cancellable.cancel();
                return true;
            }

            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.TRIGGER_THROTTLE;
        }
    }

    public static class PacketSimpleHandler extends AbstractPacketLimitHandler {


        public PacketSimpleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SIMPLE;
        }
    }

    public static class PacketSingleOverflowHandler extends AbstractPacketLimitHandler {

        private double amount;

        public PacketSingleOverflowHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if ((this.amount = amount) < getSetting().threshold) return false;

            violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SINGLE_OVERFLOW;
        }

        public double getAmount() {
            return amount;
        }
    }

    public static abstract class PacketBufferHandler extends AbstractPacketLimitHandler {

        protected double bufferIndexCopy;
        protected boolean hasDecayed;

        protected long lastDecay = System.currentTimeMillis();
        protected double bufferIndex;


        public PacketBufferHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        // put into its own method, so it can be overwritten by the buffer throttle
        public void decay() {
            bufferIndex -= getSetting().decayAmount;
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            hasDecayed = false;
            long diff = System.currentTimeMillis() - lastDecay;
            while (bufferIndex > 0 && (diff -= getSetting().decay) > 0) {
                hasDecayed = true;
                lastDecay = System.currentTimeMillis();
                decay();
            }

            if (bufferIndex < 0) {
                bufferIndex = 0;
            }
            bufferIndex += amount;

            if (bufferIndex < getSetting().threshold) {
                return false;
            }

            bufferIndexCopy = bufferIndex;
            if (getSetting().clearBuffer) {
                this.bufferIndex = 0;
            }

            return handle(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
        }

        public abstract boolean handle(Cancellable cancellable, HoneyValidation<?> validation, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey);

    }

    public static class PacketBufferThrottleHandler extends PacketBufferHandler {


        public PacketBufferThrottleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_THROTTLE;
        }

        @Override
        public void decay() {
            if (bufferIndex < getSetting().threshold) {
                super.decay();
                return;
            }
            double ratio = (int) ((bufferIndex - getSetting().threshold) / (getSetting().decayAmount * 1.8));
            double amount = getSetting().decayAmount * Math.pow(1.4, ratio);

            bufferIndex -= amount;
        }

        @Override
        public boolean handle(Cancellable cancellable, HoneyValidation<?> validation, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            cancellable.cancel();

            if (bufferIndex > getSetting().until && getSetting().isEnabled()) {
                violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            }

            return true;
        }
    }

    public static class PacketBufferOverflowHandler extends PacketBufferHandler {


        public PacketBufferOverflowHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean handle(Cancellable cancellable, HoneyValidation<?> validation, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_OVERFLOW;
        }
    }

    public static class PacketIntervalHandler extends AbstractPacketLimitHandler {

        private boolean violatedForInterval;
        private long intervalStart = System.currentTimeMillis();
        private double flagged;
        public double flaggedCopy;

        public PacketIntervalHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        private synchronized void clearInterval() {
            violatedForInterval = false;
            flagged = 0;
            intervalStart = System.currentTimeMillis();
        }

        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (System.currentTimeMillis() - intervalStart >= getSetting().interval) {
                clearInterval();
            }

            flagged += amount;
            if (flagged < getSetting().maxPerInterval) {
                return false;
            }

            cancellable.cancel();

            if (violatedForInterval) {
                return true;
            }

            violatedForInterval = true;
            flaggedCopy = flagged;
            if (getSetting().clearInterval) {
                clearInterval();
            }

            violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL;
        }
    }

    public static class PacketIntervalThrottleHandler extends AbstractPacketLimitHandler {

        private double amount;
        private boolean currentlyCancelling;
        private long intervalStart = System.currentTimeMillis();

        public PacketIntervalThrottleHandler(HoneyPlayer player, ConfigurationOptions.ViolationSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Cancellable cancellable, HoneyValidation<?> validation, double amount, Runnable onPunish, Runnable onViolation, ValidationPipelineContext.DevelopmentData developmentData, HoneyKey violationKey) {
            if (currentlyCancelling) {
                if (System.currentTimeMillis() - intervalStart > getSetting().interval) {
                    intervalStart = System.currentTimeMillis();
                    currentlyCancelling = false;
                }
                else {
                    cancellable.cancel();
                }
            }

            if ((this.amount = amount) > getSetting().maxPerInterval) {
                intervalStart = System.currentTimeMillis();

                if (amount > getSetting().until && !currentlyCancelling) {
                    violate(cancellable, validation, onPunish, onViolation, developmentData, violationKey);
                }
                currentlyCancelling = true;
                return true;
            }

            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL_THROTTLE;
        }

        public double getAmount() {
            return amount;
        }
    }
}
