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
	private String freq;
	private String tradeDate;
	private String tradeTs;
	private String isMainFuture;
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
	public String getIsMainFuture() {
		return isMainFuture;
	}
	public void setIsMainFuture(String isMainFuture) {
		this.isMainFuture = isMainFuture;
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
		tm.setFreq(m.getFreq());
		tm.setTradeDate(m.getTradeDate());
		tm.setTradeTs(m.getTradeTs());
		tm.setIsMainFuture(m.getIsMainFuture());
		return tm;
	}
	
}