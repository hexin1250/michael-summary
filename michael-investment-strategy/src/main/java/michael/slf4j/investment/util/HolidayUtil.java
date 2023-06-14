package michael.slf4j.investment.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public enum HolidayUtil {
	$;
	
	private final Logger log = Logger.getLogger(HolidayUtil.class);
	private Set<LocalDate> holiday = new TreeSet<>();
	
	public void loadHolidays() {
		try {
			String holidayFileName = "src/main/resources/holiday/holiday_cn.txt";
			Files.readAllLines(new File(holidayFileName).toPath()).stream().forEach(str -> {
				holiday.add(LocalDate.parse(str));
			});
		} catch (IOException e) {
			String errorMessage = "Error when loading holiday resources.";
			log.error(errorMessage, e);
			throw new RuntimeException(errorMessage, e);
		}
	}
	
	public boolean isHoliday(LocalDate ld) {
		return holiday.contains(ld);
	}
	
	public LocalDate getCurrentTradeDate(LocalDate ld) {
		while(isHoliday(ld) || TradeUtil.isWeekend(ld)) {
			ld = ld.plusDays(1);
		}
		return ld;
	}

}
