package michael.slf4j.investment.exception;

public class NotSubscribeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotSubscribeException() {
	}

	public NotSubscribeException(String message) {
		super(message);
	}

	public NotSubscribeException(Throwable cause) {
		super(cause);
	}

	public NotSubscribeException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotSubscribeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
