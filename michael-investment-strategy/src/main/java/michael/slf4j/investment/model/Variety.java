package michael.slf4j.investment.model;

public enum Variety {
	STOCK{{rate = 0.0003D; dayRate = 0.0003D; unit = 100D; marginRate = 1D; cal = 1;}},
	I{{rate = 0.0006D; dayRate = 0.0024D; unit = 100D; marginRate = 0.12D; cal = 1;}},
	J{{rate = 0.0006D; dayRate = 0.0024D; unit = 100D; marginRate = 0.11D; cal = 1;}},
	RB{{rate = 0.001D; dayRate = 0.002D; unit = 10D; marginRate = 0.1D; cal = 10;}};
	
	public double rate;
	public double dayRate;
	public double unit;
	public double marginRate;
	public int cal;
	
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

	public int getCal() {
		return cal;
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
