package michael.slf4j.investment.model;

import java.time.LocalTime;

public enum TradePairEnum {
	_9(LocalTime.of(9, 0, 0), LocalTime.of(10, 15, 0)),
	_10(LocalTime.of(10, 30, 0), LocalTime.of(11, 30, 0)),
	_13(LocalTime.of(13, 30, 0), LocalTime.of(15, 0, 0)),
	_21(LocalTime.of(21, 0, 0), LocalTime.of(23, 0, 0)),
	;
	
	private LocalTime start;
	private LocalTime end;
	
	private TradePairEnum(LocalTime start, LocalTime end) {
		this.start = start;
		this.end = end;
	}

	public LocalTime getStart() {
		return start;
	}

	public LocalTime getEnd() {
		return end;
	}
	
	@Override
	public String toString() {
		return start + " <-> " + end;
	}

}
