package it.bologna.ausl.minio.manager.exceptions;

/**
 *
 * @author gdm
 */
public class MinioCleanerException extends Exception{

    public MinioCleanerException(String message) {
        super(message);
    }

    public MinioCleanerException(String message, Throwable cause) {
        super(message, cause);
    }

    public MinioCleanerException(Throwable cause) {
        super(cause);
    }
    
}
