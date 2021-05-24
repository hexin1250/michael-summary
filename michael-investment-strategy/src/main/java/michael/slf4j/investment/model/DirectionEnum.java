package michael.slf4j.investment.model;

public enum DirectionEnum {
	buy{{value = 1;}},
	sell{{value = -1;}};
	
	public int value;
	
	public int getValue() {
		return value;
	}

}
