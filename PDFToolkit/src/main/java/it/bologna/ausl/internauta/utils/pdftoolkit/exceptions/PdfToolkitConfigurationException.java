package it.bologna.ausl.internauta.utils.pdftoolkit.exceptions;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
public class PdfToolkitConfigurationException extends Exception {
    
    public PdfToolkitConfigurationException(String message) {
        super(message);
    }

    public PdfToolkitConfigurationException(Throwable cause) {
        super(cause);
    }

    public PdfToolkitConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
