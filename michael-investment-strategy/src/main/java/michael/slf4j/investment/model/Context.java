package michael.slf4j.investment.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Context {
	public static final String HISTORICAL_RANGE = "historical_range";
	
	private final Account acc;
	public final long runId;
	public final Map<String, Object> params;
	public LocalDate currentTradeDate;
	
	public HistoricalBar historical;
	
	public Context(long runId, Account acc) {
		this.runId = runId;
		this.acc = acc;
		this.params = new HashMap<>();
	}
	
	public void init() {
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

	public LocalDate getCurrentTradeDate() {
		return currentTradeDate;
	}

	public void setCurrentTradeDate(LocalDate currentTradeDate) {
		this.currentTradeDate = currentTradeDate;
	}

}
