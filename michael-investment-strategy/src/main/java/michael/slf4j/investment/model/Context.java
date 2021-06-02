package michael.slf4j.investment.model;

import java.util.Map;

public class Context {
	public static final String HISTORICAL_RANGE = "historical_range";
	
	private final Account acc;
	
	public final HistoricalBar historical;
	public final Map<String, Object> params;
	
	public Context(Account acc, Map<String, Object> params) {
		this.acc = acc;
		this.params = params;
		Object hisObj = params.get(HISTORICAL_RANGE);
		int range = 0;
		if(hisObj != null) {
			range = (Integer) hisObj;
		}
		this.historical = new HistoricalBar(range);
	}

	public Account getAcc() {
		return acc;
	}

}
