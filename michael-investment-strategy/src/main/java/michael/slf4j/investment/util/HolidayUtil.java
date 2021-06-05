package michael.slf4j.investment.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public enum HolidayUtil {
	$;
	
	private final Logger log = Logger.getLogger(HolidayUtil.class);
	private Set<LocalDate> holiday = new HashSet<>();
	
	public void loadHolidays() {
		try {
			String holidayFileName = "src/main/resources/holiday/holiday_cn.txt";
			Files.readAllLines(new File(holidayFileName).toPath()).parallelStream().forEach(str -> {
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

}
