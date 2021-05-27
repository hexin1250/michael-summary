package michael.slf4j.investment.exception;

public class CashNotEnoughException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CashNotEnoughException() {
	}

	public CashNotEnoughException(String message) {
		super(message);
	}

	public CashNotEnoughException(Throwable cause) {
		super(cause);
	}

	public CashNotEnoughException(String message, Throwable cause) {
		super(message, cause);
	}

	public CashNotEnoughException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
