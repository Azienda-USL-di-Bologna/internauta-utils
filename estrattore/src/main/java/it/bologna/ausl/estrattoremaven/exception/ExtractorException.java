package it.bologna.ausl.estrattoremaven.exception;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class ExtractorException extends Exception {
private String containerFile;
private String extractingFile;

    /**
    * @param message
    */
    public ExtractorException(String message, String containerFile, String extractingFile) {
        super(message);
        this.containerFile = containerFile;
        this.extractingFile = extractingFile;
    }

    /**
    * @param cause
    */
    public ExtractorException(Throwable cause, String containerFile, String extractingFile) {
        super(cause);
        this.containerFile = containerFile;
        this.extractingFile = extractingFile;
    }
  
    /**
    * @param message
    * @param cause
    */
    public ExtractorException(String message, Throwable cause, String containerFile, String extractingFile) {
        super(message, cause);
        this.containerFile = containerFile;
        this.extractingFile = extractingFile;
    }
  
    public String getContainerFile() {
        return containerFile;
    }

    public String getExtractingFile() {
        return extractingFile;
    }
    
    @Override
    public String toString() {
        return super.toString() + "\ncontainerFile: " + containerFile + " - " + "extractractingFile: " + extractingFile;
    }
}
