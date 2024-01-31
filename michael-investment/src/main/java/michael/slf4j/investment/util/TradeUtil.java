package michael.slf4j.investment.util;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TradeUtil {
	public static LocalTime[][] periods = new LocalTime[][] {
		{LocalTime.of(21, 1, 0), LocalTime.of(23, 0, 0)},
		{LocalTime.of(9, 1, 0), LocalTime.of(10, 15, 0)},
		{LocalTime.of(10, 30, 0), LocalTime.of(11, 30, 0)},
		{LocalTime.of(13, 30, 0), LocalTime.of(15, 1, 0)}
	};
	
	public static LocalDate getLDTradeDate() {
		Date tradeDate = new Date(getTradeDate());
		return LocalDate.ofInstant(tradeDate.toInstant(), ZoneId.systemDefault());
	}
	
	public static long getTradeDate() {
		LocalDateTime ld = LocalDateTime.now();
		return getTradeDate(ld);
	}
	
	public static LocalDate previousTradeDate(LocalDate ld) {
		int dw = ld.getDayOfWeek().getValue();
		if (dw == 1) {
			return ld.minusDays(3);
		} else if (dw == 7) {
			return ld.minusDays(2);
		} else {
			return ld.minusDays(1);
		}
	}
	
	public static boolean isWeekend(LocalDate ld) {
		int dw = ld.getDayOfWeek().getValue();
		return dw == 6 || dw == 7;
	}
	
	public static long getTradeDate(Date date) {
	    LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
	    return getTradeDate(localDateTime);
	}
	
	public static long getTradeDate(LocalDateTime ld) {
		int hour = ld.getHour();
		if (hour >= 20) {
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
		return isTradingTime(LocalDateTime.now());
	}
	
	public static boolean isTradingTime(LocalDateTime ldt) {
		LocalDate ld = ldt.toLocalDate();
		DayOfWeek dayOfWeek = ld.getDayOfWeek();
		int day = dayOfWeek.getValue();
		if(day >= 6) {
			return false;
		}
		return isTradingTime(ldt.toLocalTime());
	}
	
	public static LocalDate getCurrentTradeDate(LocalDateTime ldt) {
		LocalDate ld = LocalDate.of(ldt.getYear(), ldt.getMonth(), ldt.getDayOfMonth());
		int hour = ldt.getHour();
		if(hour > 15) {
			return ld.plusDays(1);
		}
		return ld;
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
	
	public static String getDateStr(java.sql.Date tradeDate) {
		return getDateStr(tradeDate.getTime());
	}
	
	public static String getDateStr(java.util.Date tradeDate) {
		return getDateStr(tradeDate.getTime());
	}
	
	public static String getDateStr(long time) {
		return getDateStr(time, "yyyy-MM-dd");
	}
	
	public static String getDateStr(LocalDate ld) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return ld.format(formatter);
	}
	
	public static String getDateStr(long time, String pattern) {
		LocalDateTime ldt = LocalDateTime.ofInstant(new java.util.Date(time).toInstant(), ZoneId.systemDefault());
		String dateStr = ldt.format(DateTimeFormatter.ofPattern(pattern));
		return dateStr;
	}

}
