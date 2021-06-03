package michael.slf4j.investment.model;

import java.math.BigDecimal;

public class Future implements Contract {
	private final Security security;
	private final double open;
	private final double high;
	private final double low;
	private final double close;
	private final double openInterest;

	public Future(Security security, double open, double high, double low, double close, double openInterest) {
		this.security = security;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.openInterest = openInterest;
	}

	public Future(Security security, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal openInterest) {
		this(security, open.doubleValue(), high.doubleValue(), low.doubleValue(), close.doubleValue(), openInterest.doubleValue());
	}
	
	public Future(Timeseries model) {
		this(new Security(model.getSecurity(), Variety.of(model.getVariety())), model.getOpen(), model.getHigh(), model.getLow(), model.getClose(), model.getOpenInterest());
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
	
	@Override
	public String toString() {
		return close + "";
	}

	@Override
	public Security getSecurity() {
		return security;
	}

}
