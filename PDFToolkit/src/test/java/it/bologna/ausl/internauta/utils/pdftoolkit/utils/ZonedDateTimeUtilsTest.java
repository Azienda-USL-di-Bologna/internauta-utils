package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DecimalStyle;
import java.time.format.ResolverStyle;
import java.util.Locale;

import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.ZonedDateTimeUtils.SLACK_ZONED_DATE_FORMATTER;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.ZonedDateTimeUtils.ZONED_DATE_FORMATTER;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZonedDateTimeUtilsTest {
    private static ZonedDateTime zonedDateTime1;
    private static ZonedDateTime zonedDateTime2;
    private static ZonedDateTime zonedDateTime3;
    private static ZonedDateTime zonedDateTime4;
    private static ZonedDateTime zonedDateTime5;
    private static ZonedDateTime zonedDateTime6;
    private static ZonedDateTime zonedDateTime7;
    private static ZonedDateTime zonedDateTime8;
    private static ZonedDateTime zonedDateTime9;
    private static ZonedDateTime zonedDateTime10;
    private static ZonedDateTime zonedDateTime11;
    private static ZonedDateTime zonedDateTime12;
    private static ZonedDateTime zonedDateTime13;
    private static ZonedDateTime zonedDateTime14;
    private static ZonedDateTime zonedDateTime15;
    private static ZonedDateTime zonedDateTime16;
    private static ZonedDateTime zonedDateTime17;
    private static ZonedDateTime zonedDateTime18;
    private static ZonedDateTime zonedDateTime19;
    private static ZonedDateTime zonedDateTime20;
    ZoneId zoneId = ZoneId.of("Europe/Rome");

    @BeforeAll
    static void beforeAll() {
        zonedDateTime1 = ZonedDateTime.parse("2023-10-30T10:10:30.123456789+01:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime2 = ZonedDateTime.parse("2023-10-30T10:10:30.12345678+01:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime3 = ZonedDateTime.parse("2023-10-30T10:10:30.1234567+01:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime4 = ZonedDateTime.parse("2023-10-30T10:10:30.123456+01:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime5 = ZonedDateTime.parse("2023-10-30T10:10:30.12345+01:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime6 = ZonedDateTime.parse("2023-10-30T10:10:30.1234+01:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime7 = ZonedDateTime.parse("2023-10-30T10:10:30.123+01:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime8 = ZonedDateTime.parse("2023-10-30T10:10:30.12+01:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime9 = ZonedDateTime.parse("2023-10-30T10:10:30.1+01:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime10 = ZonedDateTime.parse("2023-10-30T10:10:30.+09:00[Europe/Rome]", ZONED_DATE_FORMATTER);
        zonedDateTime11 = ZonedDateTime.parse("2023-10-30T10:10:30+00:00[Europe/Rome]", ZONED_DATE_FORMATTER);

        zonedDateTime12 = ZonedDateTime.parse("2023-1-3T1:1:30.123456789+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER);
        zonedDateTime13 = ZonedDateTime.parse("2023-1-3T1:1:30.12345678+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER);
        zonedDateTime14 = ZonedDateTime.parse("2023-1-3T1:1:30.1234567+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER);
        zonedDateTime15 = ZonedDateTime.parse("2023-1-3T1:1:30.123456+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER);
        zonedDateTime16 = ZonedDateTime.parse("2023-1-3T1:1:30.12345+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER);
        zonedDateTime17 = ZonedDateTime.parse("2023-10-30T10:10:30.1234+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER);
        zonedDateTime18 = ZonedDateTime.parse("2023-10-30T10:10:30.123+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER);
        zonedDateTime19 = ZonedDateTime.parse("2023-10-30T10:10:30.12+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER);
        zonedDateTime20 = ZonedDateTime.parse("2023-10-30T10:10:30.1+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER);
    }

    @Test
    void zonedDateFormatterTest() {
        assertAll(
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 123456789, zoneId), zonedDateTime1),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 123456780, zoneId), zonedDateTime2),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 123456700, zoneId), zonedDateTime3),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 123456000, zoneId), zonedDateTime4),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 123450000, zoneId), zonedDateTime5),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 123400000, zoneId), zonedDateTime6),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 123000000, zoneId), zonedDateTime7),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 120000000, zoneId), zonedDateTime8),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 100000000, zoneId), zonedDateTime9),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 0, zoneId), zonedDateTime10),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 0, zoneId), zonedDateTime11),
                () -> assertThrows(DateTimeParseException.class, () -> ZonedDateTime.parse("2023-1-30T10:10:30+00:00[Europe/Rome]", ZONED_DATE_FORMATTER)),
                () -> assertEquals("2023-10-30", zonedDateTime2.toLocalDate().toString()),
                () -> assertEquals("2023-10-30", zonedDateTime3.toLocalDate().toString()),
                () -> assertNull(ZONED_DATE_FORMATTER.getZone()),
                () -> assertNull(ZONED_DATE_FORMATTER.getChronology()),
                () -> assertEquals(Locale.getDefault(), ZONED_DATE_FORMATTER.getLocale()),
                () -> assertEquals(DecimalStyle.STANDARD, ZONED_DATE_FORMATTER.getDecimalStyle()),
                () -> assertNull(ZONED_DATE_FORMATTER.getResolverFields()),
                () -> assertEquals(ResolverStyle.SMART, ZONED_DATE_FORMATTER.getResolverStyle())
        );
    }

    @Test
    void slackZonedDateFormatterTest() {
        assertAll(
                () -> assertEquals(ZonedDateTime.of(2023, 1, 3, 1, 1, 30, 123456789, zoneId), zonedDateTime12),
                () -> assertEquals(ZonedDateTime.of(2023, 1, 3, 1, 1, 30, 12345678, zoneId), zonedDateTime13),
                () -> assertEquals(ZonedDateTime.of(2023, 1, 3, 1, 1, 30, 1234567, zoneId), zonedDateTime14),
                () -> assertEquals(ZonedDateTime.of(2023, 1, 3, 1, 1, 30, 123456, zoneId), zonedDateTime15),
                () -> assertEquals(ZonedDateTime.of(2023, 1, 3, 1, 1, 30, 12345, zoneId), zonedDateTime16),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 1234, zoneId), zonedDateTime17),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 123, zoneId), zonedDateTime18),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 12, zoneId), zonedDateTime19),
                () -> assertEquals(ZonedDateTime.of(2023, 10, 30, 10, 10, 30, 1, zoneId), zonedDateTime20),
                () -> assertThrows(DateTimeParseException.class, () -> ZonedDateTime.parse("2023-10-30T10:10:30.+01:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER)),
                () -> assertThrows(DateTimeParseException.class, () -> ZonedDateTime.parse("2023-10-30T10:10:30+09:00[Europe/Rome]", SLACK_ZONED_DATE_FORMATTER)),
                () -> assertEquals("2023-01-03", zonedDateTime12.toLocalDate().toString()),
                () -> assertEquals("2023-10-30", zonedDateTime17.toLocalDate().toString()),
                () -> assertNull(SLACK_ZONED_DATE_FORMATTER.getZone()),
                () -> assertNull(SLACK_ZONED_DATE_FORMATTER.getChronology()),
                () -> assertEquals(Locale.getDefault(), SLACK_ZONED_DATE_FORMATTER.getLocale()),
                () -> assertEquals(DecimalStyle.STANDARD, SLACK_ZONED_DATE_FORMATTER.getDecimalStyle()),
                () -> assertNull(SLACK_ZONED_DATE_FORMATTER.getResolverFields()),
                () -> assertEquals(ResolverStyle.SMART, SLACK_ZONED_DATE_FORMATTER.getResolverStyle())
        );
    }
}