package michael.slf4j.investment.model;

public enum StrategyType {
	STOCK{{code = 1;}},
	FUTURE{{code = 2;}};
	
	public int code;
	
	public static StrategyType of(int code) {
		switch(code) {
		case 1:
			return STOCK;
		case 2:
			return FUTURE;
			default:
				return null;
		}
	}
	
	@Override
	public String toString() {
		return name();
	}


}
