package it.bologna.ausl.mongowrapper.exceptions;

public class FileDeletedExceptions  extends MongoWrapperException {
	private static final long serialVersionUID = 1L;

	public FileDeletedExceptions (String message){
		super(message);
	}

	public FileDeletedExceptions(String message, Throwable cause) {
		super (message,cause);
	}
}
