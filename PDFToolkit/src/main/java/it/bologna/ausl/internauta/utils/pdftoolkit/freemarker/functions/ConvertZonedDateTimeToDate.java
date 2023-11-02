package it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.functions;

import freemarker.template.SimpleDate;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.sql.Date;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;

/**
 * @author ferri
 */
public class ConvertZonedDateTimeToDate implements TemplateMethodModelEx {

    public static final DateTimeFormatter ZONED_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE_TIME)
            .appendFraction(NANO_OF_SECOND, 0, 9, true)
            .appendOffset("+HH:MM", "+00:00")
            .appendLiteral('[')
            .parseCaseSensitive()
            .appendZoneRegionId()
            .appendLiteral(']')
            .toFormatter();

    @Override
    public Object exec(List args) throws TemplateModelException {
        if (args.size() != 1) {
            throw new TemplateModelException("Exactly one argument is expected");
        }

        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(String.valueOf(args.get(0)), ZONED_DATE_FORMATTER);
            Date date = Date.valueOf(zonedDateTime.toLocalDate());
            return new SimpleDate(date);
        } catch (DateTimeParseException | NullPointerException | IllegalArgumentException e) {
            throw new TemplateModelException("Unable to covert argument to type date. Argument passed: " +
                    args.get(0).toString() + " expected format:" + ZONED_DATE_FORMATTER, e);
        }
    }
}
