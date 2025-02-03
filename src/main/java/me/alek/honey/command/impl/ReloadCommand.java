package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.lang.Lang;
import org.bukkit.command.CommandSender;

import java.util.concurrent.atomic.AtomicBoolean;

public class ReloadCommand implements CommandImpl {

    private final AtomicBoolean reloading = new AtomicBoolean();

    @Override
    public synchronized void perform(CommandSender sender, String[] args) {
        if (reloading.get()) {
            sender.sendMessage(Lang.getFormatted("reload.already.reloading"));
            return;
        }
        sender.sendMessage(Lang.getFormatted("reload.start"));

        reloading.set(true);
        HoneyPlugin.get().getPlugin().getServer().getScheduler().runTaskAsynchronously(HoneyPlugin.get().getPlugin(), () -> {
            HoneyPlugin.get().reload();
            sender.sendMessage(Lang.getFormatted("reload.end"));
            reloading.set(false);
        });
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return Lang.get("command.desc.reload");
    }

    @Override
    public String getPermission() {
        return "honey.command.reload";
    }

    @Override
    public boolean isExecutableByConsole() {
        return true;
    }

    @Override
    public int getCooldown() {
        return 3;
    }
}
