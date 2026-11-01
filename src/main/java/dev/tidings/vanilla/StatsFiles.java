package dev.tidings.vanilla;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class StatsFiles {
    private static final DateTimeFormatter OBTAINED =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    private StatsFiles() {}

    static UUID playerOf(Path file) {
        String name = file.getFileName().toString();
        if (!name.endsWith(".json")) return null;
        try {
            return UUID.fromString(name.substring(0, name.length() - ".json".length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static JsonObject read(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        JsonElement json = JsonParser.parseString(text);
        if (!json.isJsonObject()) throw new IOException("Not a JSON object: " + file);
        return json.getAsJsonObject();
    }

    static Map<String, Integer> customStats(JsonObject statsFile) {
        Map<String, Integer> custom = new HashMap<>();
        JsonObject stats = object(statsFile, "stats");
        JsonObject customs = stats == null ? null : object(stats, "minecraft:custom");
        if (customs == null) return custom;
        for (Map.Entry<String, JsonElement> stat : customs.entrySet()) {
            JsonElement value = stat.getValue();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                custom.put(stat.getKey(), value.getAsInt());
            }
        }
        return custom;
    }

    static Map<String, Instant> advancementsDone(JsonObject advancementsFile) {
        Map<String, Instant> done = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : advancementsFile.entrySet()) {
            String id = entry.getKey();
            if (!entry.getValue().isJsonObject() || isRecipe(id)) continue;
            JsonObject progress = entry.getValue().getAsJsonObject();
            JsonElement isDone = progress.get("done");
            if (isDone == null || !isDone.isJsonPrimitive() || !isDone.getAsBoolean()) continue;
            done.put(id, lastObtained(object(progress, "criteria")));
        }
        return done;
    }

    static boolean isRecipe(String id) {
        int colon = id.indexOf(':');
        return id.startsWith("recipes/", colon + 1);
    }

    private static Instant lastObtained(JsonObject criteria) {
        List<Instant> obtained = new ArrayList<>();
        if (criteria != null) {
            for (JsonElement at : criteria.asMap().values()) {
                try {
                    obtained.add(ZonedDateTime.parse(at.getAsString(), OBTAINED).toInstant());
                } catch (DateTimeParseException | UnsupportedOperationException e) {
                    // Not a date the game wrote
                }
            }
        }
        return PlayerStats.madeAt(obtained);
    }

    private static JsonObject object(JsonObject parent, String field) {
        JsonElement e = parent.get(field);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }
}
