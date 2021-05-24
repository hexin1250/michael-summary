package michael.slf4j.investment.model;

import java.math.BigDecimal;

public enum VarietyEnum {
	I{{rate = new BigDecimal(0.00006D); dayRate = new BigDecimal(0.00024D); unit = new BigDecimal(100); marginRate = new BigDecimal("0.12");}},
	J{{rate = new BigDecimal(0.00006D); dayRate = new BigDecimal(0.00024D); unit = new BigDecimal(100); marginRate = new BigDecimal("0.11");}},
	RB{{rate = new BigDecimal(0.0001D); dayRate = new BigDecimal(0.0002D); unit = new BigDecimal(10); marginRate = new BigDecimal("0.1");}};
	
	public BigDecimal rate;
	public BigDecimal dayRate;
	public BigDecimal unit;
	public BigDecimal marginRate;
	
	public BigDecimal getRate() {
		return rate;
	}
	public BigDecimal getDayRate() {
		return dayRate;
	}
	public BigDecimal getUnit() {
		return unit;
	}
	public BigDecimal getMarginRate() {
		return marginRate;
	}
	
	public static VarietyEnum of(String variety) {
		switch(variety) {
		case "I":
			return I;
		case "J":
			return J;
		case "RB":
			return RB;
			default:
				return null;
		}
	}

}
