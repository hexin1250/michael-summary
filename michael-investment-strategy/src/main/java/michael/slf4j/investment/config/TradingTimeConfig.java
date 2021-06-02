package michael.slf4j.investment.config;

import java.time.LocalDate;

import michael.slf4j.investment.model.TradeDateEnum;

public class TradingTimeConfig {
	private final int priority;
	private final boolean full;
	private final LocalDate start;
	private final LocalDate end;
	private final TradeDateEnum[] tradeDates;
	
	public TradingTimeConfig(String[] parts) {
		priority = Integer.valueOf(parts[1]);
		if("*".equals(parts[2]) && "*".equals(parts[3])) {
			full = true;
			start = null;
			end = null;
		} else {
			full = false;
			start = LocalDate.parse(parts[2]);
			end = LocalDate.parse(parts[3]);
		}
		int length = parts.length - 4;
		tradeDates = new TradeDateEnum[length];
		for (int i = 0; i < length; i++) {
			tradeDates[i] = TradeDateEnum.valueOf(parts[i + 4]);
		}
	}

	public int getPriority() {
		return priority;
	}

	public boolean isFull() {
		return full;
	}

	public LocalDate getStart() {
		return start;
	}

	public LocalDate getEnd() {
		return end;
	}

	public TradeDateEnum[] getTradeDates() {
		return tradeDates;
	}

}
