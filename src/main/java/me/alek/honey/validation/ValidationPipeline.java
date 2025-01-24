package me.alek.honey.validation;

import me.alek.honey.HoneyKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.util.Callback;
import me.alek.honey.validation.types.HoneyValidation;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ValidationPipeline<C extends HoneyValidation<?>> {

    public static void applyCallbackDevelopmentData(PacketContainer<?> packet, ValidationPipelineContext context) {
        context.getCallback().addAction(Callback.Action.<ViolationKey, ValidationViolationContext>from((keyOuter, contextInner) -> {
            contextInner.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                ValidationPipelineContext.DevelopmentData data = context.getDevelopmentData(keyInner);

                data.addEmpty();
                data.addValue(" Packet", packet.getType() + " (id " + packet.getType().getMinecraftID().get() + ")");
                data.addValue(" Buffer", Optional.ofNullable(packet.getInitialByteWrapped()).map(ByteInWrappedPacket::getByteSize).orElse(-1) + " bytes");
                data.addValue(" Version", Bukkit.getServer().getVersion());
                data.removeLastInternalData();
                data.addEmpty();
                data.addHeader("Description", null);

                // temporary placeholders
                data.addEmpty(); // description
                data.addEmpty(); // STQ
                data.addEmpty(); // NTQ

                if (onViolation != null) onViolation.run();
            });
        }));
    }

    public static <C extends HoneyValidation<?>> ValidationPipeline<C> from(C... validations) {
        ValidationPipeline<C> pipeline = new ValidationPipeline<>();
        Arrays.stream(validations).forEach(pipeline::register);

        return pipeline;
    }

    private final List<C> validations = new LinkedList<>();

    public void register(C validation) {
        validations.add(validation);
    }

    private ValidationViolationContext getResponseAndRegisterTimings(C validation, Function<C, ValidationViolationContext> getResponse, ConfigurationOptions.ViolationSetting setting) {
        int registryKey = -1;
        if (setting != null && setting.isEnabled()) {
            registryKey = HoneyPlugin.get().getValidationHandleTracker().registerTimeStart(validation);
        }

        ValidationViolationContext response = getResponse.apply(validation);

        if (setting != null && setting.isEnabled() && registryKey != -1) {
            HoneyPlugin.get().getValidationHandleTracker().registerTimeStop(validation, registryKey);
        }

        return response;
    }

    private void clearPosts(ValidationViolationContext context) {
        context.onViolation = null;
        context.onPunish = null;
    }

    public void doStreamOtherPipeline(HoneyPlayer player, BiFunction<C, ValidationPipelineContext, ValidationViolationContext> getResponse, Cancellable cancellable) {
        doStream(player, new ValidationPipelineContext(false), getResponse, cancellable);
    }

    public void doStream(HoneyPlayer player, ValidationPipelineContext context, BiFunction<C, ValidationPipelineContext, ValidationViolationContext> getResponse, Cancellable cancellable) {
        doStream(
                player,
                (validation, key) -> Settings.getSetting(validation),
                (c) -> getResponse.apply(c, context), cancellable, context);
    }

    public void doStream(HoneyPlayer player,
                         BiFunction<C, HoneyKey, ConfigurationOptions.ViolationSetting> getSetting,
                         Function<C, ValidationViolationContext> getServiceContext,
                         Cancellable cancellable, ValidationPipelineContext pipelineContext) {
        if (!player.isStreamActive()) {
            cancellable.cancel();
            return;
        }

        int skipping = 0;
        for (C validation : validations) {
            if (skipping-- > 0) {
                continue;
            }

            // ---------------------
            // pre requisites
            HoneyKey distributionKey = HoneyKey.of(validation);
            ConfigurationOptions.ViolationSetting setting = getSetting.apply(validation, distributionKey);

            // start to check this validation
            ValidationViolationContext defaultResponse = getResponseAndRegisterTimings(validation, getServiceContext, setting);
            List<ValidationViolationContext> responsesList = new LinkedList<>(defaultResponse.subContainers);
            responsesList.add(defaultResponse);

            HoneyKey fallbackKey = ViolationKey.of(validation);

            // handle all the responses
            boolean terminated = false;
            for (ValidationViolationContext vc : responsesList) {
                if (vc.skipIndices != 0) {
                    skipping = vc.skipIndices;
                }
                if (vc.success) {
                    if (vc.terminate) {
                        terminated = true;
                    }
                    clearPosts(vc);
                    continue;
                }
                if (vc.cancelled) {
                    cancellable.cancel();
                }
                if (vc.flag != -1 && player.isBukkitReady()) {
                    ViolationKey violationKey = new ViolationKey(validation, Optional.ofNullable(vc.violationKey).orElse(fallbackKey));
                    ValidationPipelineContext.DevelopmentData developmentData = pipelineContext.usesDevelopmentData() ? pipelineContext.getDevelopmentData(violationKey) : null;

                    if (developmentData != null) {
                        pipelineContext.getCallback().call(violationKey, vc);
                        vc.builder().onViolation(violationKey, (key, onViolation) -> {
                            if (onViolation != null) onViolation.run();

                            developmentData.setText(0, "§8* §7Player " + player.name + " flagged " + validation.getName() + " §8*");
                            developmentData.setLine(6, validation.getDescription());
                        });
                    }

                    Consumer<ViolationKey> onViolation = vc.onViolation;
                    HoneyPlugin.get().getViolationService().flag(
                            player, cancellable, distributionKey, vc.violationKey, vc.flag, vc.onPunish,
                            Optional.ofNullable(onViolation).map(o -> (Runnable) () -> o.accept(violationKey)).orElse(null),
                            validation, setting, developmentData);
                }
                clearPosts(vc);

                if (!player.isStreamActive()) {
                    cancellable.cancel();
                    return;
                }

                if (vc.terminate) {
                    terminated = true;
                }
            }

            if (terminated) {
                return;
            }
        }
    }
}
