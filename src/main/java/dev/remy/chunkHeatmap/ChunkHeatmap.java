package dev.remy.chunkHeatmap;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ChunkHeatmap extends JavaPlugin {

    // I swear, I wrote comments in this code.

    private WebServerManager webServer;
    private ChunkProfilerTask profilerTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureLogoExists();

        startWebServer();
        startProfiler();

        ChunkHeatmapCommand commandHandler = new ChunkHeatmapCommand(this);
        if (getCommand("chunkheatmap") != null) {
            getCommand("chunkheatmap").setExecutor(commandHandler);
            getCommand("chunkheatmap").setTabCompleter(commandHandler);
        }

        getLogger().info("ChunkHeatmap enabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        ensureLogoExists();

        if (webServer != null) {
            webServer.stop();
        }

        if (profilerTask != null) {
            profilerTask.cancel();
        }

        startWebServer();
        startProfiler();
        getLogger().info("ChunkHeatmap reloaded successfully.");
    }

    private void ensureLogoExists() {
        File logoFile = new File(getDataFolder(), "logo.png");
        if (!logoFile.exists()) {
            if (getResource("logo.png") != null) {
                saveResource("logo.png", false);
            } else {
                getLogger().info("no logo found or something");
            }
        }
    }

    private void startWebServer() {
        int port = getConfig().getInt("web-port", 8080); //80s80s Radio
        webServer = new WebServerManager(this);
        webServer.start(port);
    }

    private void startProfiler() {
        profilerTask = new ChunkProfilerTask(this, webServer);
        profilerTask.runTaskTimer(this, 20L, 60L);
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
        getLogger().info("ChunkHeatmap disabled.");
    }

    public WebServerManager getWebServer() {
        return webServer;
    }
}