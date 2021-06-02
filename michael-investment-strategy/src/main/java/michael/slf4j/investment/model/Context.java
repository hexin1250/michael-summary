package michael.slf4j.investment.model;

import java.util.Map;

public class Context {
	public static final String HISTORICAL_RANGE = "historical_range";
	
	private final Account acc;
	public final int runId;
	
	public HistoricalBar historical;
	
	public Context(int runId, Account acc) {
		this.runId = runId;
		this.acc = acc;
	}
	
	public void init(Map<String, Object> params) {
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
