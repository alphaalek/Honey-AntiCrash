package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.Notifications;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.sql.SQLManager;
import me.alek.honey.sql.model.ViolationEntry;
import me.alek.honey.util.pastebin.PasteRequest;
import me.alek.honey.util.pastebin.PastebinClient;
import me.alek.honey.util.pastebin.enums.Expiration;
import me.alek.honey.util.pastebin.enums.Format;
import me.alek.honey.util.pastebin.enums.Visibility;
import org.bukkit.command.CommandSender;

import java.util.Date;

public class PasteLogsCommand implements CommandImpl {

    private final SQLManager sqlHandler = HoneyPlugin.get().getSQLManager();

    public boolean handlePreconditions(CommandSender sender, String[] args) {
        if (!this.sqlHandler.isEnabled()) {
            sender.sendMessage(Lang.getFormatted("pastelogs.mysql.disabled"));
            return false;
        } else if (this.sqlHandler.isConnectionClosed()) {
            sender.sendMessage(Lang.getFormatted("pastelogs.mysql.connection.error"));
            return false;
        } else if (ConfigurationOptions.General.PASTEBIN_DEVELOPER_KEY.isEmpty()) {
            sender.sendMessage(Lang.getFormatted("pastelogs.pastebin.developer.key.error"));
            return false;
        } else if (args.length == 0) {
            sender.sendMessage(Lang.getFormatted("pastelogs.specify.player"));
            return false;
        }

        String playerName = args[0];
        if (!this.sqlHandler.getComponentBridge().existsPlayer(playerName)) {
            sender.sendMessage(Lang.getFormatted("pastelogs.unknown.player"));
            return false;
        }

        return true;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!handlePreconditions(sender, args)) {
            return;
        }

        try {
            String playerName = this.sqlHandler.getComponentBridge().getPlayerData(args[0]).getPlayerName();

            StringBuilder rawMessageBuilder = new StringBuilder();
            rawMessageBuilder
                    .append("Violation log for player ")
                    .append(playerName)
                    .append(" pasted with Honey ")
                    .append(HoneyPlugin.get().getPlugin().getDescription().getVersion())
                    .append("\n");
            int size = 0;
            for (ViolationEntry violationEntry : this.sqlHandler.getComponentBridge().getViolations(playerName)) {
                rawMessageBuilder
                        .append("[")
                        .append(new Date(violationEntry.getUnixTime()))
                        .append("] violated for ")
                        .append(violationEntry.getViolationName())
                        .append(" ")
                        .append(violationEntry.getContext())
                        .append(" [*] ").append(violationEntry.getDevData())
                        .append("\n");
                size++;
            }
            if (size == 0) {
                sender.sendMessage(Notifications.PREFIX + " §cThe player " + playerName + " doesn't have any violations.");
                return;
            }

            PasteRequest request = PasteRequest
                    .content(rawMessageBuilder.toString())
                    .visibility(Visibility.PUBLIC).format(Format.PYTHON).expiration(Expiration.ONE_DAY)
                    .name("Violation log for player " + playerName)
                    .build();
            String URL = PastebinClient.post(ConfigurationOptions.General.PASTEBIN_DEVELOPER_KEY, request);
            if (URL == null) {
                sender.sendMessage(Notifications.PREFIX + " §cAn error occurred! :( Try again later.");
                return;
            }
            String[] splittedURL = URL.split("/");
            URL = "https://" + splittedURL[splittedURL.length - 2] + "/raw/" + splittedURL[splittedURL.length - 1];

            sender.sendMessage(Notifications.PREFIX + " §7Violation log for player " + playerName + " pasted to: §e" + URL);
        } catch (Exception ex) {
            ex.printStackTrace();
            sender.sendMessage(Notifications.PREFIX + " §cA SQL error occurred :( " + (ex.getMessage() != null ? ex.getMessage() : ""));
        }
    }

    @Override
    public String getName() {
        return "pastelogs";
    }

    @Override
    public String getDescription() {
        return Lang.get("command.desc.pastelogs");
    }

    @Override
    public String getPermission() {
        return "honey.command.pastelogs";
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
