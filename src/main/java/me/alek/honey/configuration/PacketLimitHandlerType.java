package me.alek.honey.configuration;

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;

public enum PacketLimitHandlerType {

    CHIPSTATE_THROTTLE(Collections.emptySet()),

    TRIGGER_THROTTLE(Sets.newHashSet(
            ConfigurationOptions.ViolationSettingType.THROTTLE_AFTER,
            ConfigurationOptions.ViolationSettingType.VLS_AMOUNT
    )),

    INTERVAL(Sets.newHashSet(
            ConfigurationOptions.ViolationSettingType.INTERVAL,
            ConfigurationOptions.ViolationSettingType.MAX_PER_INTERVAL,
            ConfigurationOptions.ViolationSettingType.VLS_AMOUNT
    )),

    INTERVAL_THROTTLE(Sets.newHashSet(
            ConfigurationOptions.ViolationSettingType.INTERVAL,
            ConfigurationOptions.ViolationSettingType.THRESHOLD,
            ConfigurationOptions.ViolationSettingType.UNTIL,
            ConfigurationOptions.ViolationSettingType.VLS_AMOUNT
    )),

    SINGLE_OVERFLOW(Sets.newHashSet(
            ConfigurationOptions.ViolationSettingType.THRESHOLD,
            ConfigurationOptions.ViolationSettingType.VLS_AMOUNT
    )),

    BUFFER_OVERFLOW(Sets.newHashSet(
            ConfigurationOptions.ViolationSettingType.DECAY,
            ConfigurationOptions.ViolationSettingType.DECAY_AMOUNT,
            ConfigurationOptions.ViolationSettingType.THRESHOLD,
            ConfigurationOptions.ViolationSettingType.VLS_AMOUNT
    )),

    BUFFER_THROTTLE(Sets.newHashSet(
            ConfigurationOptions.ViolationSettingType.DECAY,
            ConfigurationOptions.ViolationSettingType.DECAY_AMOUNT,
            ConfigurationOptions.ViolationSettingType.THRESHOLD,
            ConfigurationOptions.ViolationSettingType.UNTIL,
            ConfigurationOptions.ViolationSettingType.VLS_AMOUNT
    )),

    MAX(Sets.newHashSet(
            ConfigurationOptions.ViolationSettingType.VLS_AMOUNT,
            ConfigurationOptions.ViolationSettingType.MAX
    )),

    MIN(Sets.newHashSet(
            ConfigurationOptions.ViolationSettingType.VLS_AMOUNT,
            ConfigurationOptions.ViolationSettingType.MIN
    )),

    SIMPLE(Sets.newHashSet(
            ConfigurationOptions.ViolationSettingType.VLS_AMOUNT
    )),
    ;

    private final Set<ConfigurationOptions.ViolationSettingType> vlSettingImplTypes;

    PacketLimitHandlerType(Set<ConfigurationOptions.ViolationSettingType> vlSettingImplTypes) {
        this.vlSettingImplTypes = vlSettingImplTypes;
    }

    public Set<ConfigurationOptions.ViolationSettingType> getVlSettingImplTypes() {
        return vlSettingImplTypes;
    }

}
