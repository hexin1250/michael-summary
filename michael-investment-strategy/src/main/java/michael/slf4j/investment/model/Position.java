package michael.slf4j.investment.model;

import java.math.BigDecimal;

import michael.slf4j.investment.util.DealUtil;
import michael.slf4j.investment.util.MathUtil;

public class Position {
	private final Security security;
	private int quantity;
	private BigDecimal dealPrice;
	private BigDecimal transactionCost;
	private BigDecimal margin;
	private BigDecimal pnl;
	
	public Position(Security security, int quantity, BigDecimal dealPrice) {
		this.security = security;
		this.quantity = quantity;
		this.dealPrice = dealPrice;
		VarietyEnum variety = security.getVariety();
		transactionCost = DealUtil.getTransactionCost(variety, dealPrice, quantity, false);
		margin = DealUtil.getMargin(variety, dealPrice, quantity);
		pnl = new BigDecimal(0);
	}
	
	public void deal(int q, BigDecimal p) {
		transactionCost = transactionCost.add(DealUtil.getTransactionCost(security.getVariety(), p, q, false));
		if(quantity * q > 0) {
			dealPrice = MathUtil.average(dealPrice, quantity, p, q);
		} else {
			int newQuantity = quantity + q;
			pnl = pnl.add(p);
		}
		quantity += q;
	}
	
	public boolean done() {
		return quantity == 0;
	}
	
	private static class DealInfo {
		private final BigDecimal price;
		private int quantity;
		public DealInfo(BigDecimal price, int quantity) {
			this.price = price;
			this.quantity = quantity;
		}
		public BigDecimal pnl(DealInfo dealInfo) {
			int min = Math.min(Math.abs(quantity), Math.abs(dealInfo.quantity));
			BigDecimal open = MathUtil.multiply(price, min * quantity / Math.abs(quantity));
			BigDecimal close = MathUtil.multiply(price, min * dealInfo.quantity / Math.abs(dealInfo.quantity));
			return MathUtil.plus(open, close).negate();
		}
		public void minus(int quantity) {
			
		}
		public boolean complete() {
			return quantity == 0;
		}
	}

}
