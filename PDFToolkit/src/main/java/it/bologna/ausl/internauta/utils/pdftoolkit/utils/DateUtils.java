package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import static java.time.temporal.ChronoField.*;

/**
 * @author ferri
 */
public class DateUtils {
    public static final DateTimeFormatter DMY_DATE_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendValue(ChronoField.DAY_OF_MONTH)
                    .appendLiteral('-')
                    .appendValue(ChronoField.MONTH_OF_YEAR)
                    .appendLiteral('-')
                    .appendValue(ChronoField.YEAR)
                    .toFormatter();

    public static final DateTimeFormatter YMD_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(YEAR, 4)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(DAY_OF_MONTH, 2)
            .toFormatter();

    public static final DateTimeFormatter DATE_WITH_SLASHES_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('/')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('/')
            .appendValue(YEAR, 4)
            .toFormatter();
}
