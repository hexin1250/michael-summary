package michael.slf4j.investment.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "timeseries")
public class Timeseries implements Comparator<Timeseries> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@Column(name = "variety")
	private String variety;
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
	@Column(name = "open_interest")
	private BigDecimal openInterest;
	@Column(name = "freq")
	private String freq;
	@Column(name = "trade_date")
	private String tradeDate;
	@Column(name = "trade_ts")
	private Timestamp tradeTs;

	public Timeseries() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getVariety() {
		return variety;
	}

	public void setVariety(String variety) {
		this.variety = variety;
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

	public Timestamp getTradeTs() {
		return tradeTs;
	}

	public void setTradeTs(Timestamp tradeTs) {
		this.tradeTs = tradeTs;
	}

	@Override
	public int compare(Timeseries o1, Timeseries o2) {
		int ret = o1.getTradeTs().compareTo(o2.getTradeTs());
		if (ret != 0) {
			return ret;
		}
		return o1.getSecurity().compareTo(o2.getSecurity());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Timeseries)) {
			return false;
		}
		Timeseries tm = (Timeseries) obj;
		return tm.getSecurityName().equals(getSecurityName()) && tm.getClose().compareTo(getClose()) == 0
				&& tm.getVolume().compareTo(getVolume()) == 0;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Security:").append(security)
			.append(", Open:").append(open)
			.append(", Close:").append(close)
			.append(", High").append(high)
			.append(", Low:").append(low)
			.append(", TS:").append(tradeTs);
		return sb.toString();
	}
	
	public Timeseries copy() {
		Timeseries ret = new Timeseries();
		ret.setClose(getClose());
		ret.setDownLimit(getDownLimit());
		ret.setFreq(getFreq());
		ret.setHigh(getHigh());
		ret.setLow(getLow());
		ret.setOpen(getOpen());
		ret.setSecurity(getSecurity());
		ret.setSecurityName(getSecurityName());
		ret.setTradeDate(getTradeDate());
		ret.setTradeTs(getTradeTs());
		ret.setUpLimit(getUpLimit());
		ret.setVariety(getVariety());
		ret.setVolume(getVolume());
		ret.setOpenInterest(getOpenInterest());
		return ret;
	}

}
