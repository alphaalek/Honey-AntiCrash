package me.alek.honey.command.impl;

import com.pastebin.api.*;
import com.pastebin.api.request.PasteRequest;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.command.CommandImpl;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.storage.SQLManagerWrapper;
import me.alek.honey.storage.model.ViolationEntry;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;

public class PasteLogsCommand implements CommandImpl {

    private final SQLManagerWrapper sqlHandler = HoneyPlugin.get().getSQLManager();

    public PasteLogsCommand(CommandManager commandManager) {
        commandManager.registerCommand(this);
    }

    public boolean handlePreconditions(CommandSender sender, String[] args) {
        if (!sqlHandler.isEnabled()) {
            sender.sendMessage(CommandManager.PREFIX + " §cYou don't have MySQL enabled in the configuration.");
            return false;
        }
        if (sqlHandler.isConnectionClosed()) {
            sender.sendMessage(CommandManager.PREFIX + " §cConnection is not established with the MySQL database. Try again later or fix the connection values.");
            return false;
        }
        if (ConfigurationOptions.General.PASTEBIN_DEVELOPER_KEY.isEmpty()) {
            sender.sendMessage(CommandManager.PREFIX + " §cYou have an empty pastebin developer key. Check the configuration for more information.");
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage(CommandManager.PREFIX + " §cYou have to specify a player!");
            return false;
        }
        final String playerName = args[0];
        if (!sqlHandler.existsPlayer(playerName)) {
            sender.sendMessage(CommandManager.PREFIX + " §cThat player does not exist in the database!");
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
                sender.sendMessage(CommandManager.PREFIX + " §acThe player " + playerName + " doesn't have any violations.");
                return;
            }

            final PasteRequest request = PasteRequest
                    .content(rawMessageBuilder.toString())
                    .visibility(Visibility.PUBLIC)
                    .format(Format.PYTHON)
                    .name("Violation log for player " + playerName)
                    .expiration(Expiration.ONE_DAY)
                    .build();
            final PastebinClient pastebinClient = PastebinClient.builder()
                    .developerKey(ConfigurationOptions.General.PASTEBIN_DEVELOPER_KEY)
                    .build();

            String URL;
            try {
                URL = pastebinClient.paste(request);
            } catch (PastebinException ex) {
                ex.printStackTrace();
                sender.sendMessage(CommandManager.PREFIX + " §cA pastebin error occurred :( " + (ex.getMessage() != null ? ex.getMessage() : ""));
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
        return "Paste the violation logs of a player.";
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
