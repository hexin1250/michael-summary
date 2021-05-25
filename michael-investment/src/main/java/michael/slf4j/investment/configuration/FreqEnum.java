package michael.slf4j.investment.configuration;

public enum FreqEnum {
	_1MI{{value = "1MI";}},
	_1D{{value = "1D";}},
	_TICK{{value = "TICK";}},
	;
	
	public String value;

	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return getValue();
	}

}
