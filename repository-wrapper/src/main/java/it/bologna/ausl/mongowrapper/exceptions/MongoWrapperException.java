package it.bologna.ausl.mongowrapper.exceptions;

public class MongoWrapperException  extends Exception {


	private static final long serialVersionUID = 1L;

	public MongoWrapperException (Throwable cause){
		super(cause);
	}
        
	public MongoWrapperException (String message){
		super(message);
	}

	public MongoWrapperException(String message, Throwable cause) {
		super (message,cause);
	}
}
