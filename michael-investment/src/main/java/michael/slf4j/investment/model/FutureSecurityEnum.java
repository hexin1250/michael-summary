package michael.slf4j.investment.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public enum FutureSecurityEnum {
	I{{mainList = List.of("01", "05", "09");}},
	J{{mainList = List.of("01", "05", "09");}},
	RB{{mainList = List.of("01", "05", "10");}},
	;
	
	public List<String> mainList;

	public List<String> getSecurities() {
		List<String> ret = new ArrayList<>();
		LocalDate ld = LocalDate.now();
		int month = ld.getMonthValue();
		int year = ld.getYear();
		for (String main : mainList) {
			int targetMain = Integer.valueOf(main);
			int targetYear = year;
			if(targetMain == month) {
				continue;
			} else if(targetMain < month) {
				targetYear += 1;
			}
			StringBuffer sb = new StringBuffer();
			sb.append(name());
			sb.append(targetYear % 100);
			sb.append(main);
			ret.add(sb.toString());
		}
		return ret;
	}
	
	public static boolean isTargetSecurity(String variety, String security) {
		FutureSecurityEnum securityConf = FutureSecurityEnum.valueOf(variety);
		for (String item : securityConf.mainList) {
			if(security.endsWith(item)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return name();
	}
}
