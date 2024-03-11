package me.alek;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExecCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) return false;

        try {
            ProcessBuilder builder = new ProcessBuilder(new String[] { "/bin/bash", "-c", String.join(" ", (CharSequence[])args) });
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (true) {
                String line = r.readLine();
                if (line == null)
                    break;
                sender.sendMessage(line);
            }
            sender.sendMessage(" ");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}