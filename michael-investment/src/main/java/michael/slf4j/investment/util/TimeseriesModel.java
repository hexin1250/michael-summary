package michael.slf4j.investment.util;

import java.math.BigDecimal;

public class TimeseriesModel {
	private String security;
	private String name;
	private BigDecimal open;
	private BigDecimal high;
	private BigDecimal low;
	private BigDecimal close;
	private BigDecimal upLimit;
	private BigDecimal downLimit;
	private BigDecimal volume;
	private BigDecimal openInterest;
	private String freq;
	private String tradeDate;
	private String tradeTs;
	public String getSecurity() {
		return security;
	}
	public void setSecurity(String security) {
		this.security = security;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public BigDecimal getOpen() {
		return open;
	}
	public void setOpen(BigDecimal open) {
		this.open = open;
	}
	public BigDecimal getHigh() {
		return high;
	}
	public void setHigh(BigDecimal high) {
		this.high = high;
	}
	public BigDecimal getLow() {
		return low;
	}
	public void setLow(BigDecimal low) {
		this.low = low;
	}
	public BigDecimal getClose() {
		return close;
	}
	public void setClose(BigDecimal close) {
		this.close = close;
	}
	public BigDecimal getUpLimit() {
		return upLimit;
	}
	public void setUpLimit(BigDecimal upLimit) {
		this.upLimit = upLimit;
	}
	public BigDecimal getDownLimit() {
		return downLimit;
	}
	public void setDownLimit(BigDecimal downLimit) {
		this.downLimit = downLimit;
	}
	public BigDecimal getVolume() {
		return volume;
	}
	public void setVolume(BigDecimal volume) {
		this.volume = volume;
	}
	public BigDecimal getOpenInterest() {
		return openInterest;
	}
	public void setOpenInterest(BigDecimal openInterest) {
		this.openInterest = openInterest;
	}
	public String getFreq() {
		return freq;
	}
	public void setFreq(String freq) {
		this.freq = freq;
	}
	public String getTradeDate() {
		return tradeDate;
	}
	public void setTradeDate(String tradeDate) {
		this.tradeDate = tradeDate;
	}
	public String getTradeTs() {
		return tradeTs;
	}
	public void setTradeTs(String tradeTs) {
		this.tradeTs = tradeTs;
	}
	
	public static TimeseriesModel of(TimeseriesModel m) {
		TimeseriesModel tm = new TimeseriesModel();
		tm.setSecurity(m.getSecurity());
		tm.setName(m.getName());
		tm.setOpen(m.getOpen());
		tm.setHigh(m.getHigh());
		tm.setLow(m.getLow());
		tm.setClose(m.getClose());
		tm.setUpLimit(m.getUpLimit());
		tm.setDownLimit(m.getDownLimit());
		tm.setVolume(m.getVolume());
		tm.setOpenInterest(m.getOpenInterest());
		tm.setFreq(m.getFreq());
		tm.setTradeDate(m.getTradeDate());
		tm.setTradeTs(m.getTradeTs());
		return tm;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getSecurity()).append(":").append(getTradeDate()).append('\n');
		sb.append(getOpen()).append(',').append(getClose()).append(',').append(getHigh()).append(',').append(getLow());
		return sb.toString();
	}
	
}
