package michael.slf4j.investment.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FutureContract {
	public static List<String> getFutureContracts(String variety){
		LocalDate ld = LocalDate.now();
		List<String> list = new ArrayList<>();
		ld = ld.minusMonths(1);
		for (int i = 0; i <= 12; i++) {
			ld = ld.plusMonths(1);
			String pattern = ld.format(DateTimeFormatter.ofPattern("yyyyMM"));
			String security = variety + pattern.substring(2);
			list.add(security);
		}
		return list;
	}

}
