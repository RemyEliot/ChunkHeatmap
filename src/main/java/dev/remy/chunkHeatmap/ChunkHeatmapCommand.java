package dev.remy.chunkHeatmap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ChunkHeatmapCommand implements CommandExecutor, TabCompleter {

    private final ChunkHeatmap plugin;

    public ChunkHeatmapCommand(ChunkHeatmap plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "--- ChunkHeatmap Commands ---");
            sender.sendMessage(ChatColor.YELLOW + "/ch reload " + ChatColor.WHITE + "- Reload configuration and web server");
            sender.sendMessage(ChatColor.YELLOW + "/ch info " + ChatColor.WHITE + "- View plugin and web server status");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "[ChunkHeatmap] Configuration and web server reloaded successfully! yay!");
            }
            case "info" -> {
                int port = plugin.getConfig().getInt("web-port", 8080);
                int clients = plugin.getWebServer() != null ? plugin.getWebServer().getConnectedClientCount() : 0;

                sender.sendMessage(ChatColor.GOLD + "--- ChunkHeatmap Info ---");
                sender.sendMessage(ChatColor.YELLOW + "developed by: " + ChatColor.RED + "RemyEliot");
                sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getPluginMeta().getVersion()); // just found out about this
                sender.sendMessage(ChatColor.YELLOW + "Web Server Port: " + ChatColor.WHITE + port);
                sender.sendMessage(ChatColor.YELLOW + "Connected Dashboard Clients: " + ChatColor.WHITE + clients);
                sender.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.GREEN + "Running"); //it always runs. 'cause if it doesn't, the command won't work.
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /ch <reload|info>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            if ("reload".startsWith(input)) completions.add("reload");
            if ("info".startsWith(input)) completions.add("info");
            return completions;
        }
        return List.of();
    }
}