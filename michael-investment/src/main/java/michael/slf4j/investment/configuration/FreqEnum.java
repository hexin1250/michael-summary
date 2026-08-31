package michael.slf4j.investment.configuration;

public enum FreqEnum {
	_1MI{{value = "1M";}},
	_5MI{{value = "5M";}},
	_15MI{{value = "15M"; period = 15 - 1;}},
	_30MI{{value = "30M"; period = 30 - 1;}},
	_1H{{value = "1H"; period = 60 - 1;}},
	_2H{{value = "2H"; period = 120 - 1;}},
	_1D{{value = "D"; period = 60 * 18 - 1;}},
	_1W{{value = "1W";period = 4 * 24 * 60 + 6 * 60 - 1;}},
	_1M{{value = "M";period = 10000L;}},
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
	
	public static FreqEnum getFreq(String value) {
		for (FreqEnum freq : values()) {
			if(value.equals(freq.getValue())) {
				return freq;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return getValue();
	}

}
