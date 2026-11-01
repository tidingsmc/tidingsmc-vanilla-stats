package dev.tidings.vanilla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class StatsFilesTest {
    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void readsCustomStatsAsTheGameSavesThem() {
        JsonObject file =
                json(
                        """
                        {"stats": {
                            "minecraft:custom": {"minecraft:deaths": 3, "minecraft:walk_one_cm": 150000},
                            "minecraft:mined": {"minecraft:stone": 40}
                        }, "DataVersion": 3465}
                        """);

        assertEquals(
                Map.of("minecraft:deaths", 3, "minecraft:walk_one_cm", 150000),
                StatsFiles.customStats(file));
    }

    @Test
    void aFileWithoutCustomStatsHasNone() {
        assertEquals(Map.of(), StatsFiles.customStats(json("{\"DataVersion\": 3465}")));
        assertEquals(Map.of(), StatsFiles.customStats(json("{\"stats\": {}}")));
    }

    @Test
    void keepsAdvancementsDoneWhenTheirLastCriterionWasMet() {
        JsonObject file =
                json(
                        """
                        {
                          "minecraft:story/mine_stone": {
                            "criteria": {"get_stone": "2026-10-30 18:04:12 +0100"},
                            "done": true
                          },
                          "minecraft:adventure/adventuring_time": {
                            "criteria": {"minecraft:plains": "2026-10-30 18:00:00 +0100"},
                            "done": false
                          },
                          "minecraft:husbandry/breed_an_animal": {
                            "criteria": {
                              "a": "2026-10-31 09:00:00 +0000",
                              "b": "2026-10-31 10:30:00 +0000"
                            },
                            "done": true
                          },
                          "minecraft:recipes/misc/torch": {
                            "criteria": {"has_stick": "2026-10-30 18:00:00 +0100"},
                            "done": true
                          },
                          "DataVersion": 3465
                        }
                        """);

        assertEquals(
                Map.of(
                        "minecraft:story/mine_stone", Instant.parse("2026-10-30T17:04:12Z"),
                        "minecraft:husbandry/breed_an_animal",
                                Instant.parse("2026-10-31T10:30:00Z")),
                StatsFiles.advancementsDone(file));
    }

    @Test
    void filesAreNamedAfterTheirPlayer() {
        UUID steve = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
        assertEquals(steve, StatsFiles.playerOf(Path.of("stats", steve + ".json")));
        assertNull(StatsFiles.playerOf(Path.of("stats", steve + ".json_old")));
        assertNull(StatsFiles.playerOf(Path.of("stats", "notes.json")));
    }

    @Test
    void anAdvancementWithoutAReadableDateWasMadeAtAnUnknownTime() {
        JsonObject file =
                json(
                        """
                        {
                          "mymod:no_criteria": {"criteria": {}, "done": true},
                          "mymod:bad_date": {"criteria": {"a": "yesterday"}, "done": true}
                        }
                        """);

        assertEquals(
                Map.of(
                        "mymod:no_criteria", PlayerStats.UNKNOWN_TIME,
                        "mymod:bad_date", PlayerStats.UNKNOWN_TIME),
                StatsFiles.advancementsDone(file));
    }

    @Test
    void playerStatsAddUpDistances() {
        PlayerStats stats =
                new PlayerStats(
                        UUID.randomUUID(),
                        "Steve",
                        Map.of(
                                "minecraft:walk_one_cm", 100,
                                "minecraft:sprint_one_cm", 50,
                                "minecraft:boat_one_cm", 1000,
                                "minecraft:fall_one_cm", 9999),
                        Map.of(
                                "minecraft:story/root",
                                Instant.EPOCH,
                                "mymod:hidden",
                                Instant.EPOCH));

        assertEquals(150, stats.walkedCm());
        assertEquals(1150, stats.travelledCm(), "falling isn't travelling");
        assertEquals(1, stats.advancementsDone(Set.of("minecraft:story/root")));
    }
}
