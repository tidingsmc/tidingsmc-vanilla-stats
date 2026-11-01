package dev.tidings.vanilla;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.authlib.GameProfile;

import dev.tidings.core.addon.AddonContext;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;

final class StatsCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsCache.class);

    private final AddonContext tidings;

    private final Map<UUID, PlayerStats> offline = new HashMap<>();

    private boolean loaded;

    /** Bumped on each server start so a load from a previous one is ignored */
    private int generation;

    StatsCache(AddonContext tidings) {
        this.tidings = tidings;
    }

    boolean loaded() {
        return loaded;
    }

    void serverStarted(MinecraftServer server) {
        int load = ++generation;
        Path stats = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        Path advancements = server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR);
        CompletableFuture.supplyAsync(() -> readAll(stats, advancements))
                .whenComplete(
                        (read, error) ->
                                server.execute(
                                        () -> {
                                            if (load != generation) return;
                                            if (error != null) {
                                                LOGGER.error("Couldn't read players' stats", error);
                                            } else {
                                                read.forEach(
                                                        (id, s) ->
                                                                offline.putIfAbsent(
                                                                        id, named(server, s)));
                                            }
                                            loaded = true;
                                        }));
    }

    void serverStopped() {
        generation++;
        offline.clear();
        loaded = false;
    }

    void playerLeft(MinecraftServer server, ServerPlayer player) {
        offline.put(player.getUUID(), live(player, Advancements.shown(server)));
    }

    /**
     * @param shown {@link Advancements#shown}
     */
    Optional<PlayerStats> of(MinecraftServer server, UUID id, List<Advancement> shown) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) return Optional.of(live(player, shown));
        return Optional.ofNullable(offline.get(id));
    }

    /**
     * @param shown {@link Advancements#shown}
     */
    List<PlayerStats> all(MinecraftServer server, List<Advancement> shown) {
        Map<UUID, PlayerStats> all = new HashMap<>(offline);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            all.put(player.getUUID(), live(player, shown));
        }
        return new ArrayList<>(all.values());
    }

    private static PlayerStats live(ServerPlayer player, List<Advancement> shown) {
        ServerStatsCounter counter = player.getStats();
        Map<String, Integer> custom = new HashMap<>();
        for (ResourceLocation stat : Stats.CUSTOM.getRegistry().keySet()) {
            int value = counter.getValue(Stats.CUSTOM.get(stat));
            if (value != 0) custom.put(stat.toString(), value);
        }
        Map<String, Instant> done = new HashMap<>();
        for (Advancement advancement : shown) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            if (progress.isDone()) {
                done.put(advancement.getId().toString(), Advancements.doneAt(progress));
            }
        }
        return new PlayerStats(player.getUUID(), player.getGameProfile().getName(), custom, done);
    }

    private static Map<UUID, PlayerStats> readAll(Path statsDir, Path advancementsDir) {
        Map<UUID, PlayerStats> all = new HashMap<>();
        for (Path file : list(statsDir)) {
            UUID id = StatsFiles.playerOf(file);
            if (id == null) continue;
            try {
                Map<String, Integer> custom = StatsFiles.customStats(StatsFiles.read(file));
                all.put(id, new PlayerStats(id, null, custom, Map.of()));
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Couldn't read {}: {}", file, e.toString());
            }
        }
        for (Path file : list(advancementsDir)) {
            UUID id = StatsFiles.playerOf(file);
            if (id == null) continue;
            try {
                Map<String, Instant> done = StatsFiles.advancementsDone(StatsFiles.read(file));
                PlayerStats stats = all.get(id);
                all.put(
                        id,
                        new PlayerStats(id, null, stats == null ? Map.of() : stats.custom(), done));
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Couldn't read {}: {}", file, e.toString());
            }
        }
        return all;
    }

    private static List<Path> list(Path dir) {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.toList();
        } catch (IOException e) {
            LOGGER.warn("Couldn't list {}: {}", dir, e.toString());
            return List.of();
        }
    }

    private PlayerStats named(MinecraftServer server, PlayerStats stats) {
        String name =
                tidings.player(stats.id())
                        .map(p -> p.name())
                        .or(
                                () ->
                                        Optional.ofNullable(server.getProfileCache())
                                                .flatMap(cache -> cache.get(stats.id()))
                                                .map(GameProfile::getName))
                        .orElse(stats.id().toString().substring(0, 8));
        return stats.withName(name);
    }
}
