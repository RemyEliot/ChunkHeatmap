package dev.remy.chunkHeatmap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebServerManager {

    private final ChunkHeatmap plugin;
    private final Set<WsContext> connectedClients = ConcurrentHashMap.newKeySet();
    private Javalin app;

    public WebServerManager(ChunkHeatmap plugin) {
        this.plugin = plugin;
    }

    public void start(int port) {
        app = Javalin.create(config -> {
            config.staticFiles.add("/web");
        }).start(port);
        app.get("/logo.png", ctx -> {
            File logoFile = new File(plugin.getDataFolder(), "logo.png");
            if (logoFile.exists()) {
                ctx.contentType("image/png");
                ctx.result(new FileInputStream(logoFile));
            } else {
                ctx.status(404);
            }
        });

        app.ws("/ws/heatmap", ws -> {
            ws.onConnect(connectedClients::add);
            ws.onClose(connectedClients::remove);
            ws.onError(connectedClients::remove);
            ws.onMessage(ctx -> handleAction(ctx.message()));
        });
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    public int getConnectedClientCount() {
        return connectedClients.size();
    }

    public void broadcast(String message) {
        for (WsContext client : connectedClients) {
            if (client.session.isOpen()) {
                client.send(message);
            }
        }
    }

    private void handleAction(String message) {
        try {
            JsonObject obj = JsonParser.parseString(message).getAsJsonObject();
            String action = obj.get("action").getAsString();
            String worldName = obj.get("world").getAsString();
            int chunkX = obj.get("chunkX").getAsInt();
            int chunkZ = obj.get("chunkZ").getAsInt();

            Bukkit.getScheduler().runTask(plugin, () -> {
                World world = Bukkit.getWorld(worldName);
                if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) return;
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                switch (action) {
                    case "PURGE_ENTITIES" -> {
                        for (Entity entity : chunk.getEntities()) {
                            if (!(entity instanceof Player)) {
                                entity.remove();
                            }
                        }
                    }
                    case "PURGE_ITEMS" -> {
                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof Item) entity.remove();
                        }
                    }
                    case "PURGE_MOBS" -> {
                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof Monster) entity.remove();
                        }
                    }
                    case "TELEPORT" -> {
                        if (obj.has("player")) {
                            Player p = Bukkit.getPlayer(obj.get("player").getAsString());
                            if (p != null) {
                                p.teleport(chunk.getBlock(8, 64, 8).getLocation());
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to execute WebSocket command: " + e.getMessage());
        }
    }
}