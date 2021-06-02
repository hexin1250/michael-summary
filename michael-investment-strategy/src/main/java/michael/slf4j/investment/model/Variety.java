package michael.slf4j.investment.model;

public enum Variety {
	I{{rate = 0.00006D; dayRate = 0.00024D; unit = 100D; marginRate = 0.12D;}},
	J{{rate = 0.00006D; dayRate = 0.00024D; unit = 100D; marginRate = 0.11D;}},
	RB{{rate = 0.0001D; dayRate = 0.0002D; unit = 10D; marginRate = 0.1D;}};
	
	public double rate;
	public double dayRate;
	public double unit;
	public double marginRate;
	
	public double getRate() {
		return rate;
	}

	public double getDayRate() {
		return dayRate;
	}

	public double getUnit() {
		return unit;
	}

	public double getMarginRate() {
		return marginRate;
	}

	public static Variety of(String variety) {
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
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name()).append(":")
			.append("Margin[").append(marginRate).append("]")
			.append(", Unit[").append(unit).append("]");
		return sb.toString();
	}

}
