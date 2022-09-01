package it.bologna.ausl.minio.manager.exceptions;

/**
 *
 * @author gdm
 */
public class MinIOWrapperException extends Exception{

    public MinIOWrapperException(String message) {
        super(message);
    }

    public MinIOWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public MinIOWrapperException(Throwable cause) {
        super(cause);
    }
    
}
