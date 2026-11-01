package dev.tidings.vanilla;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

final class Format {
    private static final long TICKS_PER_SECOND = 20;

    private Format() {}

    static Locale locale(String appLocale) {
        Locale locale = Locale.forLanguageTag(appLocale);
        return locale.getLanguage().isEmpty() ? Locale.ENGLISH : locale;
    }

    static String number(long n, Locale locale) {
        return NumberFormat.getIntegerInstance(locale).format(n);
    }

    static String duration(long ticks, Locale locale) {
        long minutes = ticks / TICKS_PER_SECOND / 60;
        if (minutes < 60) return minutes + " min";
        return decimal(minutes / 60.0, locale) + " h";
    }

    static String distance(long cm, Locale locale) {
        long meters = cm / 100;
        if (meters < 1000) return meters + " m";
        return decimal(meters / 1000.0, locale) + " km";
    }

    private static String decimal(double value, Locale locale) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMaximumFractionDigits(value < 100 ? 1 : 0);
        format.setMinimumFractionDigits(0);
        return format.format(value);
    }

    static String date(Instant instant, Locale locale) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}
