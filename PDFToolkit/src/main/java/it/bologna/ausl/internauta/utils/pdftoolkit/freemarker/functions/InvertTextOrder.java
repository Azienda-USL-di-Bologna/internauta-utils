package it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.functions;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * @author ferri
 */
public class InvertTextOrder implements TemplateMethodModelEx {

    @Override
    public Object exec(List args) throws TemplateModelException {
        if (args.size() != 1) {
            throw new TemplateModelException("Exactly one argument is expected");
        }

        try {
            StringBuilder reversedText = new StringBuilder();
            String inputText = String.valueOf(args.get(0));

            for (int i = inputText.length() - 1; i >= 0; i--) {
                reversedText.append(inputText.charAt(i));
            }

            return reversedText.toString();
        } catch (DateTimeParseException | NullPointerException | IllegalArgumentException e) {
            throw new TemplateModelException("Unable to format currency object: " + args.get(0).toString(), e);
        }
    }
}
