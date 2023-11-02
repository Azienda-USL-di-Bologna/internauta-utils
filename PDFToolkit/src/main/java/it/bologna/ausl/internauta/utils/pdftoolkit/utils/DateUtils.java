package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.*;

/**
 * @author ferri
 */
public class DateUtils {

    private final static DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .toFormatter(Locale.ITALY);

    public static String getItaCurrentIsoOffsetDate() {
        String offset = ZoneId.of("Europe/Rome").getRules().getOffset(Instant.now()).toString();

        LocalDateTime currentDate = LocalDateTime.now();

        return dateTimeFormatter.format(currentDate) + offset;
    }
}
