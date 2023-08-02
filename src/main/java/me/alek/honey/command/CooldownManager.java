package me.alek.honey.command;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class CooldownManager {

    private final Map<UUID, List<Cooldown>> cooldownMap = new HashMap<>();

    public Cooldown getCooldownModel(Player player, CommandImpl command) {
        if (!cooldownMap.containsKey(player.getUniqueId())) {
            return null;
        }
        final List<Cooldown> cooldowns = cooldownMap.get(player.getUniqueId());
        final Optional<Cooldown> cooldownOptional = cooldowns
                .stream()
                .filter(cooldown -> cooldown.getCommandImpl() == command)
                .findFirst();
        return cooldownOptional.orElse(null);
    }

    public boolean hasCooldown(Player player, CommandImpl command) {
        Cooldown cooldownModel = getCooldownModel(player, command);
        if (cooldownModel == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(cooldownModel.getWhenExpired());
    }

    public String getFormattedCooldown(Player player, CommandImpl command) {
        Cooldown cooldownModel = getCooldownModel(player, command);
        return Duration.between(LocalDateTime.now(), cooldownModel.getWhenExpired()).getSeconds() + 1 + " seconds";
    }

    public void setCooldown(Player player, CommandImpl command) {
        Cooldown cooldownModel = getCooldownModel(player, command);
        if (cooldownModel == null) {
            UUID uuid = player.getUniqueId();
            if (!this.cooldownMap.containsKey(uuid)) {
                this.cooldownMap.put(uuid, new ArrayList<>());
            }
            Cooldown newCooldownModel = new Cooldown(command, LocalDateTime.now());
            cooldownModel = newCooldownModel;
            this.cooldownMap.get(uuid).add(newCooldownModel);
        }
        cooldownModel.setCooldownNow();
    }
}
