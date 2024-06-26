package me.alek.honey.command.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.sql.SQLManagerWrapper;
import me.alek.honey.sql.model.ViolationEntry;
import me.alek.honey.util.pastebin.PasteRequest;
import me.alek.honey.util.pastebin.PastebinClient;
import me.alek.honey.util.pastebin.enums.Expiration;
import me.alek.honey.util.pastebin.enums.Format;
import me.alek.honey.util.pastebin.enums.Visibility;
import org.bukkit.command.CommandSender;

import java.util.Date;

public class PasteLogsCommand implements CommandImpl {

    private final SQLManagerWrapper sqlHandler = HoneyPlugin.get().getSQLManager();

    public boolean handlePreconditions(CommandSender sender, String[] args) {
        if (!sqlHandler.isEnabled()) {
            sender.sendMessage(Lang.getFormatted("pastelogs.mysql.disabled"));
            return false;
        }
        if (sqlHandler.isConnectionClosed()) {
            sender.sendMessage(Lang.getFormatted("pastelogs.mysql.connection.error"));
            return false;
        }
        if (ConfigurationOptions.General.PASTEBIN_DEVELOPER_KEY.isEmpty()) {
            sender.sendMessage(Lang.getFormatted("pastelogs.pastebin.developer.key.error"));
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage(Lang.getFormatted("pastelogs.specify.player"));
            return false;
        }
        final String playerName = args[0];
        if (!sqlHandler.existsPlayer(playerName)) {
            sender.sendMessage(Lang.getFormatted("pastelogs.unknown.player"));
            return false;
        }
        return true;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!handlePreconditions(sender, args)) return;

        try {
            final String playerName = sqlHandler.getPlayerData(args[0]).getPlayerName();

            final StringBuilder rawMessageBuilder = new StringBuilder();
            rawMessageBuilder.append("Violation log for player " + playerName + " pasted with Honey " + HoneyPlugin.get().getPlugin().getDescription().getVersion() + "\n");

            int size = 0;
            for (ViolationEntry violationEntry : sqlHandler.getViolations(playerName)) {
                rawMessageBuilder.append("[" + new Date(violationEntry.getUnixTime()) + "] violated for " + violationEntry.getViolationName() + " " + violationEntry.getContext() + " [*] " + violationEntry.getDevData() + "\n");
                size++;
            }
            if (size == 0) {
                sender.sendMessage(CommandManager.PREFIX + " §cThe player " + playerName + " doesn't have any violations.");
                return;
            }

            final PasteRequest request = PasteRequest
                    .content(rawMessageBuilder.toString())
                    .visibility(Visibility.PUBLIC).format(Format.PYTHON).expiration(Expiration.ONE_DAY)
                    .name("Violation log for player " + playerName)
                    .build();
            String URL = PastebinClient.post(ConfigurationOptions.General.PASTEBIN_DEVELOPER_KEY, request);
            if (URL == null) {
                sender.sendMessage(CommandManager.PREFIX + " §cAn error occurred! :( Try again later.");
                return;
            }

            String[] splittedURL = URL.split("/");
            URL = "https://" + splittedURL[splittedURL.length - 2] + "/raw/" + splittedURL[splittedURL.length - 1];

            sender.sendMessage(CommandManager.PREFIX + " §7Violation log for player " + playerName + " pasted to: §e" + URL);
        } catch (Exception ex) {
            ex.printStackTrace();
            sender.sendMessage(CommandManager.PREFIX + " §cA SQL error occurred :( " + (ex.getMessage() != null ? ex.getMessage() : ""));
        }
    }

    @Override
    public String getName() {
        return "pastelogs";
    }

    @Override
    public String getUsage() {
        return "/honey pastelogs";
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
