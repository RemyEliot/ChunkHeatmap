package dev.remy.chunkHeatmap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChunkProfilerTask extends BukkitRunnable {

    private final ChunkHeatmap plugin;
    private final WebServerManager webServer;

    private record ChunkSnapshotData(
            String worldName,
            int x,
            int z,
            int players,
            int mobs,
            int items,
            int tileEntities
    ) {}

    public ChunkProfilerTask(ChunkHeatmap plugin, WebServerManager webServer) {
        this.plugin = plugin;
        this.webServer = webServer;
    }

    @Override
    public void run() {
        FileConfiguration config = plugin.getConfig();
        double mobPts = config.getDouble("points.mob", 2.0);
        double itemPts = config.getDouble("points.item", 0.5);
        double tilePts = config.getDouble("points.tile-entity", 1.0);

        List<ChunkSnapshotData> snapshot = new ArrayList<>(500);

        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();

            for (Chunk chunk : world.getLoadedChunks()) {
                try {
                    if (!chunk.isLoaded()) continue;

                    int players = 0, mobs = 0, items = 0;
                    for (Entity entity : chunk.getEntities()) {
                        if (entity instanceof Player) players++;
                        else if (entity instanceof Monster) mobs++;
                        else if (entity instanceof Item) items++;
                    }

                    int tileEntities = chunk.getTileEntities().length;

                    snapshot.add(new ChunkSnapshotData(
                            worldName, chunk.getX(), chunk.getZ(),
                            players, mobs, items, tileEntities
                    ));
                } catch (Exception ignored) {
                }
            }
        }

        //Pain.™

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            StringBuilder json = new StringBuilder(snapshot.size() * 120 + 500);
            json.append("{\"type\":\"HEATMAP_UPDATE\",\"timestamp\":")
                    .append(System.currentTimeMillis() / 1000L);

            json.append(",\"thresholds\":[");
            List<Map<?, ?>> thresholds = config.getMapList("color-thresholds");
            for (int i = 0; i < thresholds.size(); i++) {
                Map<?, ?> map = thresholds.get(i);
                if (i > 0) json.append(',');
                json.append("{\"minHeat\":").append(map.get("min-heat"))
                        .append(",\"color\":\"").append(map.get("color")).append("\"}");
            }
            json.append("]");

            json.append(",\"chunks\":[");
            for (int i = 0; i < snapshot.size(); i++) {
                ChunkSnapshotData c = snapshot.get(i);
                double heat = (c.mobs() * mobPts) + (c.items() * itemPts) + (c.tileEntities() * tilePts);

                if (i > 0) json.append(',');
                json.append("{\"x\":").append(c.x())
                        .append(",\"z\":").append(c.z())
                        .append(",\"world\":\"").append(c.worldName())
                        .append("\",\"heat\":").append(heat)
                        .append(",\"players\":").append(c.players())
                        .append(",\"mobs\":").append(c.mobs())
                        .append(",\"items\":").append(c.items())
                        .append(",\"tileEntities\":").append(c.tileEntities())
                        .append('}');
            }
            json.append("]}");

            webServer.broadcast(json.toString());
        });
    }
}