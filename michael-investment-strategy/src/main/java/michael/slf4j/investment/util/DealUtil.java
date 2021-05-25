package michael.slf4j.investment.util;

import java.math.BigDecimal;

import michael.slf4j.investment.model.VarietyEnum;

public class DealUtil {
	public static BigDecimal getMargin(VarietyEnum variety, BigDecimal dealPrice, int quantity) {
		return new BigDecimal(quantity).multiply(dealPrice).multiply(variety.getMarginRate()).multiply(variety.getUnit()).abs();
	}
	
	public static BigDecimal getTransactionCost(VarietyEnum variety, BigDecimal dealPrice, int quantity, boolean isDayTrade) {
		BigDecimal rate = isDayTrade ? variety.getDayRate() : variety.getRate();
		return new BigDecimal(quantity).multiply(dealPrice).multiply(variety.getUnit()).multiply(rate).abs();
	}

}
