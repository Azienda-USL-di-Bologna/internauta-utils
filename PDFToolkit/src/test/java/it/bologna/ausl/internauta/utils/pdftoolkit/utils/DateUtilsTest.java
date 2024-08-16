package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DecimalStyle;
import java.time.format.ResolverStyle;
import java.util.Locale;

import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.DateUtils.DMY_DATE_FORMATTER;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DateUtilsTest {
    @Test
    void dateFormatterTest() {
        LocalDate localDate1 = LocalDate.parse("1-2-1993", DMY_DATE_FORMATTER);
        LocalDate localDate2 = LocalDate.parse("01-2-1993", DMY_DATE_FORMATTER);
        LocalDate localDate3 = LocalDate.parse("01-02-1993", DMY_DATE_FORMATTER);
        LocalDate localDate4 = LocalDate.parse("001-002-1993", DMY_DATE_FORMATTER);
        LocalDate localDate5 = LocalDate.parse("01-02-0001993", DMY_DATE_FORMATTER);
        LocalDate localDate6 = LocalDate.parse("0031-02-1993", DMY_DATE_FORMATTER);
        LocalDate localDate7 = LocalDate.parse("1-2-199300000", DMY_DATE_FORMATTER);
        LocalDate localDate8 = LocalDate.parse("01-02--1993", DMY_DATE_FORMATTER);
        LocalDate localDate9 = LocalDate.parse("01-02--000199300000", DMY_DATE_FORMATTER);
        LocalDate localDate10 = LocalDate.parse("01-2-0", DMY_DATE_FORMATTER);
        assertAll(
                () -> assertThrows(DateTimeParseException.class, () -> LocalDate.parse("0-2-1993", DMY_DATE_FORMATTER)),
                () -> assertThrows(DateTimeParseException.class, () -> LocalDate.parse("01-0-1993", DMY_DATE_FORMATTER)),
                () -> assertThrows(DateTimeParseException.class, () -> LocalDate.parse("--", DMY_DATE_FORMATTER)),
                () -> assertThrows(DateTimeParseException.class, () -> ZonedDateTime.parse("30-11-2023", DMY_DATE_FORMATTER)),
                () -> assertEquals(LocalDate.of(1993, 2, 1), localDate1),
                () -> assertEquals(LocalDate.of(1993, 2, 1), localDate2),
                () -> assertEquals(LocalDate.of(1993, 2, 1), localDate3),
                () -> assertEquals(LocalDate.of(1993, 2, 1), localDate4),
                () -> assertEquals(LocalDate.of(1993, 2, 1), localDate5),
                () -> assertEquals(LocalDate.of(1993, 2, 28), localDate6),
                () -> assertEquals(LocalDate.of(+199300000, 2, 1), localDate7),
                () -> assertEquals(LocalDate.of(-1993, 2, 1), localDate8),
                () -> assertEquals(LocalDate.of(-199300000, 2, 1), localDate9),
                () -> assertEquals(LocalDate.of(0, 2, 1), localDate10),
                () -> assertNull(DMY_DATE_FORMATTER.getZone()),
                () -> assertNull(DMY_DATE_FORMATTER.getChronology()),
                () -> assertEquals(Locale.getDefault(), DMY_DATE_FORMATTER.getLocale()),
                () -> assertEquals(DecimalStyle.STANDARD, DMY_DATE_FORMATTER.getDecimalStyle()),
                () -> assertNull(DMY_DATE_FORMATTER.getResolverFields()),
                () -> assertEquals(ResolverStyle.SMART, DMY_DATE_FORMATTER.getResolverStyle())
        );
    }
}