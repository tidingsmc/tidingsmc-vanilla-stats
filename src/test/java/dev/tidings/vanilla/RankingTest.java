package dev.tidings.vanilla;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RankingTest {
    @Test
    void linksNameRankingsByTheirId() {
        for (Ranking ranking : Ranking.values()) assertEquals(ranking, Ranking.byId(ranking.id));
    }

    @Test
    void anEditedLinkFallsBackToDeaths() {
        assertEquals(Ranking.DEATHS, Ranking.byId("nonsense"));
    }
}
