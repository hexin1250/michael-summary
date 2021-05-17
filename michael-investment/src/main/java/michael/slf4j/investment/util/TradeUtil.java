package michael.slf4j.investment.util;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TradeUtil {
	private static LocalTime[][] periods = new LocalTime[][] {
		{LocalTime.of(9, 0, 0), LocalTime.of(10, 15, 0)},
		{LocalTime.of(10, 30, 0), LocalTime.of(11, 30, 0)},
		{LocalTime.of(13, 30, 0), LocalTime.of(15, 0, 0)},
		{LocalTime.of(21, 0, 0), LocalTime.of(23, 0, 0)}
	};
	
	public static long getTradeDate() {
		LocalDateTime ld = LocalDateTime.now();
		return getTradeDate(ld);
	}
	
	public static long getTradeDate(Date date) {
	    LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
	    return getTradeDate(localDateTime);
	}
	
	private static long getTradeDate(LocalDateTime ld) {
		int hour = ld.getHour();
		if (hour > 20) {
			int dw = ld.getDayOfWeek().getValue();
			if (dw == 5) {
				ld = ld.plusDays(3);
			} else if (dw == 6) {
				ld = ld.plusDays(2);
			} else {
				ld = ld.plusDays(1);
			}
		}
		return Date.from(ld.atZone(ZoneId.systemDefault()).toInstant()).getTime();
	}

	public static boolean isTradingTime() {
		return isTradingTime(LocalTime.now());
	}

	public static boolean isTradingTime(LocalTime ldt) {
		for (LocalTime[] localTimes : periods) {
			LocalTime start = localTimes[0];
			LocalTime end = localTimes[1];
			if((start.getHour() == ldt.getHour() && start.getMinute() <= ldt.getMinute()) || (end.getHour() == ldt.getHour() && end.getMinute() >= ldt.getMinute()) || (start.getHour() < ldt.getHour() && ldt.getHour() < end.getHour())) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isCompleteMunite() {
		LocalTime ldt = LocalTime.now();
		return ldt.getSecond() == 0;
	}
	
	public static String getDateStr(Date tradeDate) {
		return getDateStr(tradeDate.getTime());
	}
	
	public static String getDateStr(long time) {
		LocalDateTime ldt = LocalDateTime.ofInstant(new java.util.Date(time).toInstant(), ZoneId.systemDefault());
		String dateStr = ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		return dateStr;
	}

}
