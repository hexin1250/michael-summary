package michael.slf4j.investment.quant;

import michael.slf4j.investment.model.Variety;

public class BacktestRequest {
	private final Variety variety;
	private final String freq;
	private final String startDate;
	private final String endDate;
	public BacktestRequest(Variety variety, String freq, String startDate, String endDate) {
		this.variety = variety;
		this.freq = freq;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	public Variety getVariety() {
		return variety;
	}
	public String getFreq() {
		return freq;
	}
	public String getStartDate() {
		return startDate;
	}
	public String getEndDate() {
		return endDate;
	}

}
