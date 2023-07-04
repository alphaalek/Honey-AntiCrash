package me.alek.honey.command;

import java.time.LocalDateTime;

public class CooldownMdl {

    private LocalDateTime localDateTime;
    private final CommandImpl commandImpl;

    public CooldownMdl(CommandImpl commandImpl, LocalDateTime localDateTime) {
        this.commandImpl = commandImpl;
        this.localDateTime = localDateTime;
    }

    public LocalDateTime getWhenExpired() {
        return localDateTime;
    }

    public CommandImpl getCommandImpl() {
        return commandImpl;
    }

    public void setCooldownNow() {
        localDateTime = LocalDateTime.now().plusSeconds(commandImpl.getCooldown());
    }
}
