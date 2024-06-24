package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.*;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.CallbackAction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CheckPipeline<C extends HoneyValidation<?>> {

    private final List<C> validations = new LinkedList<>();
    private final HoneyPlayer player;

    public static <C extends HoneyValidation<?>> CheckPipeline<C> from(HoneyPlayer player, C... checks) {
        CheckPipeline<C> pipeline = new CheckPipeline<>(player);
        for (C check : checks) {
            pipeline.register(check);
        }
        return pipeline;
    }

    public CheckPipeline(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public void register(C check) {
        validations.add(check);
    }

    public List<C> getValidations() {
        return validations;
    }

    private ResponseContainer getResponseAndRegisterTimings(C check, Function<C, ResponseContainer> getResponse, ConfigurationOptions.VlSetting setting) {
        long registryKey = -1;
        if (setting != null && setting.isEnabled()) {
            registryKey = HoneyPlugin.get().getCheckHandleTracker().registerTimeStart(check.getName());
        }

        ResponseContainer response = getResponse.apply(check);

        if (setting != null && setting.isEnabled() && registryKey != -1) {
            HoneyPlugin.get().getCheckHandleTracker().registerTimeStop(check.getName(), registryKey);
        }

        return response;
    }

    public void doStream(HoneyPlayer player, Object handle,
                          BiFunction<C, ViolationService.HoneyKey, ConfigurationOptions.VlSetting> getSetting,
                          Function<C, ResponseContainer> getResponse,
                          Cancellable cancellable, CheckContextContainer checkContextContainer, CallbackAction<ResponseContainer> callback) {
        int skipping = 0;
        for (C check : validations) {
            if (skipping-- > 0) {
                continue;
            }

            // ---------------------
            // pre requisites
            ViolationService.HoneyKey key = ViolationService.HoneyKey.of(check);
            ConfigurationOptions.VlSetting setting = getSetting.apply(check, key);
            // ---------------------

            // start to check this validation
            ResponseContainer defaultResponse = getResponseAndRegisterTimings(check, getResponse, setting);
            List<ResponseContainer> responsesList = new LinkedList<>(defaultResponse.subContainers);
            responsesList.add(defaultResponse);

            // handle all the responses
            boolean terminated = false;
            CheckContextContainer.DevelopmentData developmentData = checkContextContainer.getDevelopmentData(check);
            for (ResponseContainer response : responsesList) {
                if (response == null) {
                    continue;
                }
                if (response.skipIndices != 0) {
                    skipping = response.skipIndices;
                }
                if (response.success) {
                    if (response.terminate) {
                        terminated = true;
                    }
                    continue;
                }
                if (response.cancelled) {
                    cancellable.cancel();
                }
                if (response.flag != -1 && player != null) {
                    if (setting == null) {
                        if (response.terminate) {
                            terminated = true;
                        }
                        continue;
                    }
                    if (response.onViolation == null && response.onPunish == null && response.onFlag == null) {
                        response = response.copyWithoutPosts().build();
                    }

                    callback.call(response);
                    response.builder().onViolation((c, onViolation) -> {
                        if (onViolation != null) onViolation.run();

                        developmentData.setText(0, "§8* §7Player " + player.name + " flagged " + check.getName() + " §8*");
                        developmentData.setLine(5, check.getDescription());
                    });

                    ResponseContainer finalResponse = response;
                    HoneyPlugin.get().getViolationService().flag(
                            player, handle, cancellable, key, response.violationKey, response.flag,
                            response.onPunish, () -> finalResponse.onViolation.accept(check), response.onFlag, check, setting, developmentData);

                    if (player.punished) {
                        cancellable.cancel();
                        return;
                    }
                }

                if (response.punish && player != null) {
                    HoneyPlugin.get().getViolationService().punish(null, player, check, key);
                    return;
                }

                if (response.terminate) {
                    terminated = true;
                }
            }

            if (terminated) {
                return;
            }
        }
    }
}
