package michael.slf4j.investment.util;

import michael.slf4j.investment.model.Variety;

public class DealUtil {
	public static double getMargin(Variety variety, double dealPrice, int quantity) {
		return dealPrice * quantity * variety.getMarginRate() * variety.getUnit();
	}
	
	public static double getTransactionCost(Variety variety, double dealPrice, int quantity, boolean isDayTrade) {
		if(variety == Variety.STOCK) {
			return getStockTransactionCost(variety, dealPrice, quantity, isDayTrade);
		}
		double rate = isDayTrade ? variety.getDayRate() : variety.getRate();
		return dealPrice * quantity * variety.getUnit() * rate;
	}
	
	private static double getStockTransactionCost(Variety variety, double dealPrice, int quantity, boolean isDayTrade) {
		double rate = isDayTrade ? variety.getDayRate() : variety.getRate();
		double cost = dealPrice * quantity * variety.getUnit() * rate;
		return cost < 6D ? 6D : cost;
	}

}
