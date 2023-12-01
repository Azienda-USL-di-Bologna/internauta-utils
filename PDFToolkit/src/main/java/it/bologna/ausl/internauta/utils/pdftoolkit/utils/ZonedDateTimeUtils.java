package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import static java.time.temporal.ChronoField.*;

/**
 * @author ferri
 */
public final class ZonedDateTimeUtils {
    public static final DateTimeFormatter SLACK_ZONED_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .parseStrict()
            .appendValue(ChronoField.YEAR)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE)
            .appendLiteral('.')
            .appendValue(NANO_OF_SECOND)
            .appendOffset("+HH:MM", "+00:00")
            .appendLiteral('[')
            .parseCaseSensitive()
            .appendZoneRegionId()
            .appendLiteral(']')
            .toFormatter();

    public static final DateTimeFormatter ZONED_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .appendOffset("+HH:MM", "+00:00")
            .appendLiteral('[')
            .parseCaseSensitive()
            .appendZoneRegionId()
            .appendLiteral(']')
            .toFormatter();
}
