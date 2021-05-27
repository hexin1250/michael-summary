package michael.slf4j.investment.exception;

public class InvalidCloseException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidCloseException() {
	}

	public InvalidCloseException(String message) {
		super(message);
	}

	public InvalidCloseException(Throwable cause) {
		super(cause);
	}

	public InvalidCloseException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidCloseException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
