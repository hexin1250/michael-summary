package michael.slf4j.investment.model;

public class ProfitLoss {
	private final double margin;
	private final double pnl;
	private final double transactionCost;
	
	public ProfitLoss(double margin, double pnl, double transactionCost) {
		this.margin = margin;
		this.pnl = pnl;
		this.transactionCost = transactionCost;
	}

	public double getMargin() {
		return margin;
	}

	public double getPnl() {
		return pnl;
	}

	public double getTransactionCost() {
		return transactionCost;
	}
}
