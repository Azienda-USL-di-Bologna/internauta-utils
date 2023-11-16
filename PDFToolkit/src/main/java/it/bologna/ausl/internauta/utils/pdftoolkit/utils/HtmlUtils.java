package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ferri
 */
public class HtmlUtils {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HtmlUtils.class);

    public static List<String> getFontFamilies(String htmlContent) {
        List<String> fontList = new ArrayList<>();
        int startIndex = htmlContent.indexOf("<style>");
        int endIndex = htmlContent.indexOf("</style>");

        if (startIndex != -1 && endIndex != -1) {
            String styleBlock = htmlContent.substring(startIndex + "<style>".length(), endIndex);
            int position = 0;

            while (position < styleBlock.length()) {
                int fontFamilyIndex = styleBlock.indexOf("font-family:", position);
                if (fontFamilyIndex == -1) {
                    break;
                }

                int valueStart = fontFamilyIndex + "font-family:".length();
                int semicolonIndex = styleBlock.indexOf(";", valueStart);
                String value = styleBlock.substring(valueStart, semicolonIndex)
                        .replace("!important", "")
                        .replace("\"", "").trim();

                if (!value.isEmpty()) {
                    fontList.add(value);
                }

                position = semicolonIndex;
            }
        }

        return fontList.stream().distinct().collect(Collectors.toList());
    }
}
