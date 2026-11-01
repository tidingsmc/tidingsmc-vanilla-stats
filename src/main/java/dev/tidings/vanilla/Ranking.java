package dev.tidings.vanilla;

import java.util.Locale;
import java.util.Set;

enum Ranking {
    DEATHS("deaths", "Deaths"),
    PLAY_TIME("playtime", "Play time"),
    WALKED("walked", "Walked"),
    MOB_KILLS("kills", "Mob kills"),
    ADVANCEMENTS("advancements", "Advancements");

    final String id;

    final String label;

    Ranking(String id, String label) {
        this.id = id;
        this.label = label;
    }

    static Ranking byId(String id) {
        for (Ranking ranking : values()) {
            if (ranking.id.equals(id)) return ranking;
        }
        return DEATHS;
    }

    /**
     * @param shown the advancements the game shows
     */
    long value(PlayerStats stats, Set<String> shown) {
        return switch (this) {
            case DEATHS -> stats.deaths();
            case PLAY_TIME -> stats.playTimeTicks();
            case WALKED -> stats.walkedCm();
            case MOB_KILLS -> stats.mobKills();
            case ADVANCEMENTS -> stats.advancementsDone(shown);
        };
    }

    String format(long value, Locale locale) {
        return switch (this) {
            case DEATHS, MOB_KILLS, ADVANCEMENTS -> Format.number(value, locale);
            case PLAY_TIME -> Format.duration(value, locale);
            case WALKED -> Format.distance(value, locale);
        };
    }
}
