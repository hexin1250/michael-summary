package michael.slf4j.investment.configuration;

public enum FreqEnum {
	_1MI{{value = "1MI";}},
	_15MI{{value = "15M"; period = 15 - 1;}},
	_30MI{{value = "30M"; period = 30 - 1;}},
	_1H{{value = "1H"; period = 60 - 1;}},
	_2H{{value = "2H"; period = 120 - 1;}},
	_1D{{value = "1D"; period = 60 * 18 - 1;}},
	_TICK{{value = "TICK";}},
	;
	
	public String value;
	public long period;

	public String getValue() {
		return value;
	}
	
	public long getPeriod() {
		return period;
	}

	@Override
	public String toString() {
		return getValue();
	}

}
