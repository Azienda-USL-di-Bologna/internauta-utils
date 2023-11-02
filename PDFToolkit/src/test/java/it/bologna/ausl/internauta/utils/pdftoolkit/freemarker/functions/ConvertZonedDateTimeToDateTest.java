package it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.functions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConvertZonedDateTimeToDateTest {

    @Test
    void dateTimeFormatterTest() {
        DateTimeFormatter dateTimeFormatter = ConvertZonedDateTimeToDate.ZONED_DATE_FORMATTER;
        assertEquals(ResolverStyle.SMART, dateTimeFormatter.getResolverStyle());
        String example1 = "2023-10-30T10:10:30." + new Random().nextInt(1000000000) + "+01:00[Europe/Rome]";
        String example2 = "2023-10-30T10:10:30.+09:00[Europe/Rome]";
        String example3 = "2023-10-30T10:10:30+00:00[Europe/Rome]";
        ZonedDateTime zonedDateTime1 = ZonedDateTime.parse(example1, dateTimeFormatter);
        assertEquals("2023-10-30", zonedDateTime1.toLocalDate().toString());
        ZonedDateTime zonedDateTime2 = ZonedDateTime.parse(example2, dateTimeFormatter);
        assertEquals("2023-10-30", zonedDateTime2.toLocalDate().toString());
        ZonedDateTime zonedDateTime3 = ZonedDateTime.parse(example3, dateTimeFormatter);
        assertEquals("2023-10-30", zonedDateTime3.toLocalDate().toString());
    }
}