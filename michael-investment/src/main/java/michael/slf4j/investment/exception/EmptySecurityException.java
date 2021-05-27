package michael.slf4j.investment.exception;

public class EmptySecurityException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EmptySecurityException() {
	}

	public EmptySecurityException(String message) {
		super(message);
	}

	public EmptySecurityException(Throwable cause) {
		super(cause);
	}

	public EmptySecurityException(String message, Throwable cause) {
		super(message, cause);
	}

	public EmptySecurityException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
