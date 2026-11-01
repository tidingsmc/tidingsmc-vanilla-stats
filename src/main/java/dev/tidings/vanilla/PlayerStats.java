package dev.tidings.vanilla;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What vanilla Minecraft knows about a player: their statistics and the advancements they made.
 *
 * @param custom the "minecraft:custom" statistics, such as "minecraft:deaths", by ID
 * @param advancements when each advancement was made, by ID, or {@link #UNKNOWN_TIME}; only the
 *     ones done, without recipes
 */
public record PlayerStats(
        UUID id, String name, Map<String, Integer> custom, Map<String, Instant> advancements) {

    private static final List<String> WALKING =
            List.of("minecraft:walk_one_cm", "minecraft:sprint_one_cm", "minecraft:crouch_one_cm");

    private static final List<String> TRAVELLING =
            List.of(
                    "minecraft:walk_one_cm",
                    "minecraft:sprint_one_cm",
                    "minecraft:crouch_one_cm",
                    "minecraft:swim_one_cm",
                    "minecraft:walk_on_water_one_cm",
                    "minecraft:walk_under_water_one_cm",
                    "minecraft:climb_one_cm",
                    "minecraft:fly_one_cm",
                    "minecraft:aviate_one_cm",
                    "minecraft:boat_one_cm",
                    "minecraft:minecart_one_cm",
                    "minecraft:horse_one_cm",
                    "minecraft:pig_one_cm",
                    "minecraft:strider_one_cm");

    public static final Instant UNKNOWN_TIME = Instant.EPOCH;

    public PlayerStats {
        custom = Map.copyOf(custom);
        advancements = Map.copyOf(advancements);
    }

    static Instant madeAt(Collection<Instant> criteriaMet) {
        return criteriaMet.stream().max(Comparator.naturalOrder()).orElse(UNKNOWN_TIME);
    }

    public PlayerStats withName(String name) {
        return new PlayerStats(id, name, custom, advancements);
    }

    public long custom(String id) {
        return custom.getOrDefault(id, 0);
    }

    public long deaths() {
        return custom("minecraft:deaths");
    }

    public long playTimeTicks() {
        return custom("minecraft:play_time");
    }

    public long walkedCm() {
        return WALKING.stream().mapToLong(this::custom).sum();
    }

    public long travelledCm() {
        return TRAVELLING.stream().mapToLong(this::custom).sum();
    }

    public long mobKills() {
        return custom("minecraft:mob_kills");
    }

    public long playerKills() {
        return custom("minecraft:player_kills");
    }

    public long advancementsDone(Set<String> shown) {
        return advancements.keySet().stream().filter(shown::contains).count();
    }
}
