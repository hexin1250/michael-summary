package michael.slf4j.investment.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "real_run_txn")
public class RealRunTxn implements Comparator<RealRunTxn>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "real_run_id")
	private Long realRunId;
	@Column(name = "security")
	private String security;
	@Column(name = "variety")
	private String variety;
	@Column(name = "deal_price")
	private BigDecimal dealPrice;
	@Column(name = "deal_count")
	private Integer dealCount;
	@Column(name = "direction")
	private Integer direction;
	@Column(name = "trade_date")
	private String tradeDate;
	@Column(name = "trade_ts")
	private Timestamp tradeTs;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getRealRunId() {
		return realRunId;
	}

	public void setRealRunId(Long realRunId) {
		this.realRunId = realRunId;
	}

	public String getSecurity() {
		return security;
	}

	public void setSecurity(String security) {
		this.security = security;
	}

	public String getVariety() {
		return variety;
	}

	public void setVariety(String variety) {
		this.variety = variety;
	}

	public BigDecimal getDealPrice() {
		return dealPrice;
	}

	public void setDealPrice(BigDecimal dealPrice) {
		this.dealPrice = dealPrice;
	}

	public Integer getDealCount() {
		return dealCount;
	}

	public void setDealCount(Integer dealCount) {
		this.dealCount = dealCount;
	}

	public Integer getDirection() {
		return direction;
	}

	public void setDirection(Integer direction) {
		this.direction = direction;
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
	public int compare(RealRunTxn o1, RealRunTxn o2) {
		int ret = o1.getTradeTs().compareTo(o2.getTradeTs());
		if (ret != 0) {
			return ret;
		}
		return o1.getSecurity().compareTo(o2.getSecurity());
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(realRunId).append(",").append(security)
			.append(",").append(variety)
			.append(",").append(dealPrice)
			.append(",").append(dealCount)
			.append(",").append(direction)
			.append(",").append(tradeDate)
			.append(",").append(tradeTs);
		return sb.toString();
	}

}
