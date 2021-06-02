package michael.slf4j.investment.model;

public enum DirectionEnum {
	buy{{value = -1; display = "Buy Open";}},
	sell{{value = 1; display = "Sell Open";}},
	buy_close{{value = -1; display = "Buy Close";}},
	sell_close{{value = 1; display = "Sell Close";}};
	
	public int value;
	public String display;
	public DirectionEnum pair;
	
	public int getValue() {
		return value;
	}
	
	public DirectionEnum getPair() {
		switch(this) {
		case buy:
			return sell_close;
		case sell:
			return buy_close;
		case sell_close:
			return buy;
		case buy_close:
			return sell;
		default:
			return null;
		}
	}

	@Override
	public String toString() {
		return display;
	}

}
