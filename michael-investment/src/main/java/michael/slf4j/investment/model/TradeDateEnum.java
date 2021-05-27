package michael.slf4j.investment.model;

import static michael.slf4j.investment.model.TradePairEnum.*;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum TradeDateEnum {
	night(_21),
	day(_9, _10, _13),
	;
	
	private TradePairEnum[] tradingHours;
	
	private TradeDateEnum(TradePairEnum... pairs) {
		this.tradingHours = pairs;
	}

	public TradePairEnum[] getTradingHours() {
		return tradingHours;
	}
	
	@Override
	public String toString() {
		return name() + ":" + Arrays.stream(tradingHours).map(pair -> pair.toString()).collect(Collectors.joining(","));
	}

}
