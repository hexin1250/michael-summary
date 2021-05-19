package michael.slf4j.investment.quant;

public class BacktestRequest {
	private final String variety;
	private final String freq;
	private final String startDate;
	private final String endDate;
	public BacktestRequest(String variety, String freq, String startDate, String endDate) {
		this.variety = variety;
		this.freq = freq;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	public String getVariety() {
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
