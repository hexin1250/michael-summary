package michael.slf4j.investment.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class TradeDeal implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Security security;
	private final boolean direction;
	private final int amount;
	private final BigDecimal price;
	private final Timestamp ts;
	
	public TradeDeal(Security security, boolean direction, BigDecimal price, int amount, Timestamp ts) {
		this.security = security;
		this.direction = direction;
		this.price = price;
		this.amount = amount;
		this.ts = ts;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Security getSecurity() {
		return security;
	}

	public boolean isDirection() {
		return direction;
	}

	public int getAmount() {
		return amount;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public Timestamp getTs() {
		return ts;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append((direction ? "Buy" : "Sell"))
			.append(" ").append(security)
			.append(" ").append(amount).append("unit(s)")
			.append(" with ").append(price)
			.append(" at [").append(ts).append("]");
		return sb.toString();
	}

}
