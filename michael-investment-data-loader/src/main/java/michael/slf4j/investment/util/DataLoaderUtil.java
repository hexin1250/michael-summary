package michael.slf4j.investment.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Timeseries;

public class DataLoaderUtil {
	public static List<Timeseries> generate30TsListBy15ForRealTime(List<Timeseries> series) {
		List<Timeseries> list = new ArrayList<Timeseries>();
		Timeseries ts = null;
		for (Timeseries min15Ts : series) {
			Timestamp timestamp = min15Ts.getTradeTs();
			LocalDateTime ldt = TradeUtil.getLocalDateTime(timestamp);
			int hour = ldt.getHour();
			int min = ldt.getMinute();
			boolean sameCondition = false;
			if(min % 30 == 0 || (hour == 10 && min == 15) || ts == null) {
				ts = min15Ts.copy();
				if(min % 15 == 0) {
					ts.setVolume(new BigDecimal(0));
				}
				sameCondition = true;
			}
			if(min % 30 == 15) {
				ts.setOpen(min15Ts.getOpen());
				ts.setHigh(new BigDecimal(Math.max(ts.getHigh().doubleValue(), min15Ts.getHigh().doubleValue())));
				ts.setLow(new BigDecimal(Math.min(ts.getLow().doubleValue(), min15Ts.getLow().doubleValue())));
				ts.setVolume(ts.getVolume().add(min15Ts.getVolume()));
				ts.setFreq(FreqEnum._30MI.getValue());
				if(sameCondition) {
					LocalDateTime newLdt = ldt.plusMinutes(15);
					ts.setTradeTs(new Timestamp(TradeUtil.getLong(newLdt)));
				}
				list.add(ts);
				ts = null;
			}
		}
		return list;
	}
	
	public static List<Timeseries> generate30TsListBy15ForBack(List<Timeseries> series) {
		Map<Timestamp, Timeseries> map = new TreeMap<>();
		for (Timeseries min15Ts : series) {
			Timestamp timestamp = min15Ts.getTradeTs();
			LocalDateTime ldt = TradeUtil.getLocalDateTime(timestamp);
			int min = ldt.getMinute();
			if(min % 30 == 15) {
				LocalDateTime newLdt = ldt.plusMinutes(15);
				timestamp = new Timestamp(TradeUtil.getLong(newLdt));
			}
			Timeseries ts = map.get(timestamp);
			if(ts == null) {
				ts = min15Ts.copy();
				ts.setTradeTs(timestamp);
				map.put(timestamp, ts);
			} else {
				ts.setClose(min15Ts.getClose());
				ts.setHigh(new BigDecimal(Math.max(ts.getHigh().doubleValue(), min15Ts.getHigh().doubleValue())));
				ts.setLow(new BigDecimal(Math.min(ts.getLow().doubleValue(), min15Ts.getLow().doubleValue())));
			}
			ts.setFreq(FreqEnum._30MI.getValue());
		}
		return map.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
	}
	
	public static List<Timeseries> generate60TsListBy30ForBack(List<Timeseries> series) {
		Map<Timestamp, Timeseries> map = new TreeMap<>();
		for (Timeseries min30Ts : series) {
			Timestamp timestamp = min30Ts.getTradeTs();
			LocalDateTime ldt = TradeUtil.getLocalDateTime(timestamp);
			int hour = ldt.getHour();
			int min = ldt.getMinute();
			if(min == 30) {
				int plusMinute = 30;
				if(hour == 11) {
					plusMinute += 120;
				}
				LocalDateTime newLdt = ldt.plusMinutes(plusMinute);
				timestamp = new Timestamp(TradeUtil.getLong(newLdt));
			}
			Timeseries ts = map.get(timestamp);
			if(ts == null) {
				ts = min30Ts.copy();
				ts.setTradeTs(timestamp);
				map.put(timestamp, ts);
			} else {
				ts.setClose(min30Ts.getClose());
				ts.setHigh(new BigDecimal(Math.max(ts.getHigh().doubleValue(), min30Ts.getHigh().doubleValue())));
				ts.setLow(new BigDecimal(Math.min(ts.getLow().doubleValue(), min30Ts.getLow().doubleValue())));
			}
			ts.setFreq(FreqEnum._1H.getValue());
		}
		return map.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
	}

}
