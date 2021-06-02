package michael.slf4j.investment.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import michael.slf4j.investment.config.TradingTimeConfig;
import michael.slf4j.investment.model.TradeDateEnum;
import michael.slf4j.investment.model.Variety;

public class TradingTimeUtil {
	private static final Logger log = Logger.getLogger(TradingTimeUtil.class);
	
	private static Map<String, List<TradingTimeConfig>> map = new HashMap<>();
	
	static {
		try {
			String holidayFileName = "src/main/resources/trading/trading.time.txt";
			Files.readAllLines(new File(holidayFileName).toPath()).stream().forEach(line -> {
				String[] parts = line.split(",");
				TradingTimeConfig config = new TradingTimeConfig(parts);
				List<TradingTimeConfig> list = map.get(parts[0]);
				if(list == null) {
					list = new ArrayList<>();
					map.put(parts[0], list);
				}
				list.add(config);
			});
		} catch (IOException e) {
			String errorMessage = "Error when loading trading time resources.";
			log.error(errorMessage, e);
			throw new RuntimeException(errorMessage, e);
		}
	}
	
	public static TradeDateEnum[] getTradingTime(Variety variety, LocalDate tradeDate) {
		List<TradingTimeConfig> list = map.get(variety.name());
		if(list != null) {
			for (TradingTimeConfig config : list) {
				if(config.isFull()) {
					return config.getTradeDates();
				}
				if(tradeDate.compareTo(config.getStart()) >= 0 && tradeDate.compareTo(config.getEnd()) <= 0) {
					return config.getTradeDates();
				}
			}
		}
		List<TradingTimeConfig> mainList = map.get("*");
		for (TradingTimeConfig config : mainList) {
			if(config.isFull()) {
				return config.getTradeDates();
			}
			if(tradeDate.compareTo(config.getStart()) >= 0 && tradeDate.compareTo(config.getEnd()) <= 0) {
				return config.getTradeDates();
			}
		}
		return null;
	}

}
