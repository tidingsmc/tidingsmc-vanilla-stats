package dev.tidings.vanilla;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Locale;

class FormatTest {
    private static final Locale EN = Format.locale("en");
    private static final Locale FR = Format.locale("fr");
    private static final long MINUTE = 20 * 60;

    @Test
    void durationsInMinutesThenHours() {
        assertEquals("0 min", Format.duration(0, EN));
        assertEquals("45 min", Format.duration(45 * MINUTE, EN));
        assertEquals("1.5 h", Format.duration(90 * MINUTE, EN));
        assertEquals("1,5 h", Format.duration(90 * MINUTE, FR));
        assertEquals("250 h", Format.duration(250 * 60 * MINUTE + 10 * MINUTE, EN));
    }

    @Test
    void distancesInMetersThenKilometers() {
        assertEquals("850 m", Format.distance(85_000, EN));
        assertEquals("3.2 km", Format.distance(320_000, EN));
        assertEquals("3 km", Format.distance(300_000, EN));
        assertEquals("1,234 km", Format.distance(123_400_000, EN));
    }

    @Test
    void unknownLocalesFallBackToEnglish() {
        assertEquals(Locale.ENGLISH, Format.locale(""));
        assertEquals("1,000", Format.number(1000, Format.locale("")));
    }
}
