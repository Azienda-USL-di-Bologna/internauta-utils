package it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.functions;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * @author ferri
 */
public class FormatFloatToCurrency implements TemplateMethodModelEx {

    @Override
    public Object exec(List args) throws TemplateModelException {
        if (args.size() != 1) {
            throw new TemplateModelException("Exactly one argument is expected");
        }
        try {
            Float floatValue = Float.valueOf(args.get(0).toString());
            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            decimalFormat.setRoundingMode(RoundingMode.DOWN);
            return decimalFormat.format(floatValue);
        } catch (DateTimeParseException | NullPointerException | IllegalArgumentException e) {
            throw new TemplateModelException("Unable to format currency object: " + args.get(0).toString(), e);
        }
    }
}
