package michael.slf4j.investment.model;

import java.math.BigDecimal;

public class Future implements Contract {
	private final double open;
	private final double high;
	private final double low;
	private final double close;
	private final double openInterest;

	public Future(double open, double high, double low, double close, double openInterest) {
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.openInterest = openInterest;
	}

	public Future(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal openInterest) {
		this(open.doubleValue(), high.doubleValue(), low.doubleValue(), close.doubleValue(), openInterest.doubleValue());
	}
	
	public Future(Timeseries model) {
		this(model.getOpen(), model.getHigh(), model.getLow(), model.getClose(), model.getOpenInterest());
	}

	@Override
	public double getLow() {
		return low;
	}

	@Override
	public double getHigh() {
		return high;
	}

	@Override
	public double getOpen() {
		return open;
	}

	@Override
	public double getClose() {
		return close;
	}

	@Override
	public double getOpenInterest() {
		return openInterest;
	}

}
