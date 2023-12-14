package it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.functions;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.List;

import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.DateUtils.DATE_WITH_SLASHES_FORMATTER;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.DateUtils.YMD_DATE_FORMATTER;

/**
 * @author ferri
 */
public class FormatDateWIthSlashes implements TemplateMethodModelEx {

    @Override
    public Object exec(List args) throws TemplateModelException {
        if (args.size() != 1) {
            throw new TemplateModelException("Exactly one argument is expected");
        }
        try {
            TemporalAccessor temporalAccessor = YMD_DATE_FORMATTER.parse(String.valueOf(args.get(0)));
            LocalDate localDate = LocalDate.from(temporalAccessor);
            return localDate.format(DATE_WITH_SLASHES_FORMATTER);
        } catch (DateTimeParseException | NullPointerException | IllegalArgumentException e) {
            throw new TemplateModelException("Unable to format currency object: " + args.get(0).toString(), e);
        }
    }
}