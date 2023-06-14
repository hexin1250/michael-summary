package michael.slf4j.investment.model;

public enum DirectionEnum {
	buy{{value = -1; display = "Buy Open"; code = 1;}},
	sell{{value = 1; display = "Sell Open"; code = 2;}},
	buy_close{{value = -1; display = "Buy Close"; code = 11;}},
	sell_close{{value = 1; display = "Sell Close"; code = 12;}};
	
	public int value;
	public String display;
	public DirectionEnum pair;
	public int code;
	
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
	
	public static DirectionEnum of(int code) {
		switch(code) {
		case 1:
			return buy;
		case 2:
			return sell;
		case 11:
			return buy_close;
		case 12:
			return sell_close;
			default:
				return null;
		}
	}

	@Override
	public String toString() {
		return display;
	}

}
