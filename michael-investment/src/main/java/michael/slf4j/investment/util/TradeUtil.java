package michael.slf4j.investment.util;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TradeUtil {
	public static long getTradeDate() {
		LocalDateTime ld = LocalDateTime.now();
		int hour = ld.getHour();
		if(hour > 20) {
			int dw = ld.getDayOfWeek().getValue();
			if(dw == 5) {
				ld = ld.plusDays(3);
			} else if(dw == 6) {
				ld = ld.plusDays(2);
			} else {
				ld = ld.plusDays(1);
			}
		}
		return Date.from(ld.atZone(ZoneId.systemDefault()).toInstant()).getTime();
	}
}
