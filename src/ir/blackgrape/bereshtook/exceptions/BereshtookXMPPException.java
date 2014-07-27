package ir.blackgrape.bereshtook.exceptions;

public class BereshtookXMPPException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public BereshtookXMPPException(String message) {
		super(message);
	}

	public BereshtookXMPPException(String message, Throwable cause) {
		super(message, cause);
	}
}
