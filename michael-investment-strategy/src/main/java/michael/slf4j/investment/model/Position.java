package michael.slf4j.investment.model;

import java.math.BigDecimal;

import michael.slf4j.investment.util.DealUtil;

public class Position {
	private final Security security;
	private final DirectionEnum direction;
	private int quantity;
	private BigDecimal dealPrice;
	private BigDecimal transactionCost;
	private BigDecimal margin;
	private BigDecimal pnl;
	
	public Position(Security security, DirectionEnum direction, int quantity, BigDecimal dealPrice) {
		this.security = security;
		this.direction = direction;
		this.quantity = quantity;
		this.dealPrice = dealPrice;
		VarietyEnum variety = security.getVariety();
		transactionCost = DealUtil.getTransactionCost(variety, dealPrice, quantity, false);
		pnl = new BigDecimal(0);
	}
	
	public void deal(DirectionEnum d, int q, BigDecimal p) {
		
	}

}
