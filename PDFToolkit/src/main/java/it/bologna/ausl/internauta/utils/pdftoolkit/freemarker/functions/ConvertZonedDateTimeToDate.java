package it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.functions;

import freemarker.template.SimpleDate;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.sql.Date;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.ZonedDateTimeUtils.ZONED_DATE_FORMATTER;
import java.time.format.DateTimeFormatter;

/**
 * @author ferri
 */
public class ConvertZonedDateTimeToDate implements TemplateMethodModelEx {

    @Override
    public Object exec(List args) throws TemplateModelException {
        if (args.size() != 1) {
            throw new TemplateModelException("Exactly one argument is expected");
        }

        SimpleDate res;
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(String.valueOf(args.get(0)), ZONED_DATE_FORMATTER);
            Date date = Date.valueOf(zonedDateTime.toLocalDate());
            res = new SimpleDate(date);
        } catch (DateTimeParseException | NullPointerException | IllegalArgumentException e) {
            try {
                DateTimeFormatter alternativePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(String.valueOf(args.get(0)), alternativePattern);
                Date date = Date.valueOf(zonedDateTime.toLocalDate());
                res = new SimpleDate(date);
            } catch (DateTimeParseException | NullPointerException | IllegalArgumentException subEx) {
                throw new TemplateModelException("Unable to covert argument to type date. Argument passed: " +
                    args.get(0).toString() + " expected format:" + ZONED_DATE_FORMATTER, subEx);
            }
        }
        return res;
    }
}
