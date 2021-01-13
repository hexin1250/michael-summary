package michael.slf4j.investment.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name="timeseries")
public class TimeseriesModel implements Comparator<TimeseriesModel> {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;
	@Column(name = "security")
	private String security;
	@Column(name = "security_name")
	private String securityName;
	@Column(name = "open")
	private BigDecimal open;
	@Column(name = "high")
	private BigDecimal high;
	@Column(name = "low")
	private BigDecimal low;
	@Column(name = "close")
	private BigDecimal close;
	@Column(name = "up_limit")
	private BigDecimal upLimit;
	@Column(name = "down_limit")
	private BigDecimal downLimit;
	@Column(name = "volume")
	private BigDecimal volume;
	@Column(name = "freq")
	private String freq;
	@Column(name = "trade_date")
	private Date tradeDate;
	@Column(name = "trade_ts")
	private Timestamp tradeTs;
	@Column(name = "is_main_future")
	private String isMainFuture;
	public TimeseriesModel() {
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getSecurity() {
		return security;
	}
	public void setSecurity(String security) {
		this.security = security;
	}
	public String getSecurityName() {
		return securityName;
	}
	public void setSecurityName(String securityName) {
		this.securityName = securityName;
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
	public Date getTradeDate() {
		return tradeDate;
	}
	public void setTradeDate(Date tradeDate) {
		this.tradeDate = tradeDate;
	}
	public Timestamp getTradeTs() {
		return tradeTs;
	}
	public void setTradeTs(Timestamp tradeTs) {
		this.tradeTs = tradeTs;
	}
	public String getIsMainFuture() {
		return isMainFuture;
	}
	public void setIsMainFuture(String isMainFuture) {
		this.isMainFuture = isMainFuture;
	}
	@Override
	public int compare(TimeseriesModel o1, TimeseriesModel o2) {
		int ret = o1.getTradeTs().compareTo(o2.getTradeTs());
		if(ret != 0) {
			return ret;
		}
		return o1.getSecurity().compareTo(o2.getSecurity());
	}

}
