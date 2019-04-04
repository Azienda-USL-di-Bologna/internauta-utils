package it.bologna.ausl.eml.handler;

public class EmlHandlerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EmlHandlerException (String message){
		super(message);
	}

	public EmlHandlerException(String message, Throwable cause) {
		
		super (message,cause);
	}
        
//        public EmlHandlerResultException (String message) {
//		super (message);
//	}
}